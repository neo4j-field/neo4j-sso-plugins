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
package org.neo4j.sso.auth.plugin;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.*;

import org.jose4j.jwk.HttpsJwks;
import org.jose4j.jwk.JsonWebKey;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.keys.resolvers.HttpsJwksVerificationKeyResolver;

import com.neo4j.server.security.enterprise.auth.plugin.api.AuthProviderOperations;
import com.neo4j.server.security.enterprise.auth.plugin.api.AuthToken;
import com.neo4j.server.security.enterprise.auth.plugin.api.AuthenticationException;
import com.neo4j.server.security.enterprise.auth.plugin.spi.AuthInfo;
import com.neo4j.server.security.enterprise.auth.plugin.spi.AuthPlugin;
import org.jose4j.lang.JoseException;

public class AADJWTAuthPlugin extends AuthPlugin.Adapter
{
    private AuthProviderOperations api;

    private String issuer  ;
    private String audience ;
    private String jwksURL ;
    private String userNameField = "unique_name" ;
    private String groupsClaimName = "groups" ;

    private boolean useGroupMapping = false ;
    private Properties groupMapping = null ;

    @Override
    public String name() {
    	return "oauth-token-validator" ;
    }
    
    @Override
    public AuthInfo authenticateAndAuthorize( AuthToken authToken ) throws AuthenticationException
    {
        String username = authToken.principal();       
        char[] password = authToken.credentials();
        String passwordstr = new String (password);
    	Map<String, Object> tokenUser = null;
        Set<String> neo4JRoles = new HashSet<>();


        if (passwordstr != null )
        {
        	api.log().info( "Log in attempted for user '" + username + "'.");

        	String access_token = passwordstr ;

		    HttpsJwks httpsJkws = new HttpsJwks(jwksURL);
		    HttpsJwksVerificationKeyResolver httpsJwksKeyResolver = new HttpsJwksVerificationKeyResolver(httpsJkws);
		    JwtConsumer jwtConsumer = new JwtConsumerBuilder()
		            .setRequireExpirationTime() // the JWT must have an expiration time
		            .setAllowedClockSkewInSeconds(3600)
		            .setRequireSubject() // the JWT must have a subject claim
		            .setExpectedIssuer(issuer) // whom the JWT needs to have been issued by
		            .setExpectedAudience(audience) // to whom the JWT is intended for
		            .setVerificationKeyResolver(httpsJwksKeyResolver)
		            .build();

		    try
		    {
		        //  Validate the JWT and process the Claims
		        JwtClaims jwtClaims = jwtConsumer.processToClaims(access_token);
		        api.log().info("JWT validation succeeded!");
		        api.log().info(jwtClaims.toString());

		        if(jwtClaims != null) {
		            tokenUser = new HashMap<String, Object>();
		            tokenUser.put("name", jwtClaims.getClaimValue("name"));
		            tokenUser.put("username", jwtClaims.getClaimValue(userNameField));
                    Collection o = jwtClaims.getStringListClaimValue(groupsClaimName);
                    System.out.println("Groups : " + o );
                    if( o != null ) {
                        tokenUser.put(groupsClaimName, o);
                    }
		        }

		        String user = (String) tokenUser.get("username");

		        @SuppressWarnings("unchecked")
				Collection roles = (Collection) tokenUser.get(groupsClaimName);
//                api.log().info("useGroupMapping: " + useGroupMapping ) ;
//                api.log().info("groupMapping: " + groupMapping ) ;
		        for (Object s: roles) {
                    String t = s.toString() ;
                    if( useGroupMapping && groupMapping != null ) {
                        t = groupMapping.getProperty(t) ;
                    }
                    if( t != null ) {
                        neo4JRoles.add(t);
                    }
		        }

		        api.log().info("Neo4j Roles for "+user+" are : "+neo4JRoles);
		        return(AuthInfo.of(user, neo4JRoles));
		    }

		    catch (InvalidJwtException e)
		    {
		        e.printStackTrace();
		    	api.log().error("Invalid JWT! " + e);
		    } catch (MalformedClaimException e) {
				e.printStackTrace();
		    } catch (Exception e) {
		        e.printStackTrace();
            }

        }
        
        return null;
    }

    @Override
    public void initialize( AuthProviderOperations authProviderOperations )
    {
        api = authProviderOperations;

        loadConfig();
        api.log().info( "initialized!" );


    }

    private void loadConfig()
    {
        Path configFile = resolveConfigFilePath("oauth.conf");
        Properties properties = loadProperties( configFile );

        String groupMapFile = properties.getProperty( "auth.oauth.groupsMapping" );
        jwksURL = properties.getProperty( "auth.oauth.jwksURL" );
		audience = properties.getProperty( "auth.oauth.expectedAudience" );
		issuer = properties.getProperty( "auth.oauth.expectedIssuer" );

		String useGroups = properties.getProperty( "auth.oauth.useGroupMapping" );
		if( useGroups != null && useGroups.trim().toLowerCase().equals("true")) {
		    useGroupMapping = true ;
        }
		if( groupMapFile != null ) {
		    groupMapping = loadProperties(resolveConfigFilePath(groupMapFile)) ;
        }
		String value = null ;
		value = properties.getProperty( "auth.oauth.userName" );
		if( value != null && !value.isBlank()) {
		    userNameField = value ;
        }
        value = properties.getProperty( "auth.oauth.groupsClaim" );
        if( value != null && !value.isBlank()) {
            groupsClaimName = value ;
        }
    }

    private Path resolveConfigFilePath(String filename)
    {
        return api.neo4jHome().resolve( "conf/" + filename );
    }

    private Properties loadProperties( Path configFile )
    {
        Properties properties = new Properties();

        try
        {
            InputStream inputStream = new FileInputStream( configFile.toFile() );
            properties.load( inputStream );
        }
        catch ( IOException e )
        {
            api.log().error( "Failed to load config file '" + configFile.toString() + "'." );
        }
        return properties;
    }
}
