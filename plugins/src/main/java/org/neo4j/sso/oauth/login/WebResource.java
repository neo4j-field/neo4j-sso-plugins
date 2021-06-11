/**
 * Copyright (c) 2002-2017 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.neo4j.sso.oauth.login;

import org.neo4j.configuration.Config;
import org.neo4j.graphdb.config.Setting;
import org.neo4j.logging.Log;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.FileInputStream;
import java.io.InputStream;

@Path("/")
public class WebResource {

    public static java.nio.file.Path configPath = null ;

    @Context
    Log log ;

    @Context
    Config config ;

    @GET
    @Path("/")
    public Response file() {
        log.error("In Auth Web Resource Serving file : ");
        return file("index.html");
    }

    @GET
    @Path("{file:(?i).+\\.(png|jpg|jpeg|svg|gif|html?|js|css|txt|grass)}")
    public Response file(@PathParam("file") String file) {
//        Setting<Object> setting = config.getSetting("auth.oauth.sso.files") ;
//
//        InputStream fileStream = findFileStream(setting.defaultValue().toString(), file);
        InputStream fileStream = findFileStream(configPath.toString()+"/sso", file);
        if (fileStream == null)
            return Response.status(Response.Status.NOT_FOUND).build();
        else
            return Response.ok(fileStream, mediaType(file)).build();
    }

    private InputStream findFileStream(String dir, String path) {
        String file = (new StringBuilder()).append(dir).append("/").append(path).toString() ;
        log.error("Auth Web Resource Serving file : " + file);
        InputStream fis = null ;
        try {
            fis = new FileInputStream(file) ;
        } catch (Exception e) {

        }
        log.error("Auth Web Resource Serving file : " + fis);
        return fis;
    }

    public String mediaType(String file) {
        int dot = file.lastIndexOf(".");
        if (dot == -1) return MediaType.TEXT_PLAIN;
        String ext = file.substring(dot + 1).toLowerCase();
        switch (ext) {
            case "png":
                return "image/png";
            case "gif":
                return "image/gif";
            case "json":
                return MediaType.APPLICATION_JSON;
            case "js":
                return "text/javascript";
            case "css":
                return "text/css";
            case "svg":
                return "image/svg+xml";
            case "html":
                return MediaType.TEXT_HTML;
            case "txt":
                return MediaType.TEXT_PLAIN;
            case "jpg":
            case "jpeg":
                return "image/jpg";
            default:
                return MediaType.TEXT_PLAIN;
        }
    }
}