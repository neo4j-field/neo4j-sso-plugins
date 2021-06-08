# neo4j-oauth-token-validator-plugin
OAuth Token validator plugin to validate the token and do the optional group name mapping. If no group mapping file is provided then the names from the token are used as it is.

You have to build this project with Java 11.

If you just want to build the plugins, you can choose to ignore integration tests by running:

    mvn clean install -DskipITs 

## Install plugins in Neo4j
Copy the output jar file into the plugins folder of Neo4j Enterprise Edition 4.0 or later:

    cp plugins/target/oauth-sso-token-validator-plugin-<VERSION>.jar <NEO4J-HOME>/plugins/

Edit the Neo4j configuration file `<NEO4J-HOME>/conf/neo4j.conf` and add the `dbms.security.authentication_providers` 
and `dbms.security.authorization_providers` settings, e.g.:

    dbms.security.authentication_providers=plugin-oauth-token-validator
    dbms.security.authorization_providers=plugin-oauth-token-validator

You can also enable multiple plugins simultaneously e.g.:

    dbms.security.authentication_providers=native,plugin-oauth-token-validator

## Configuration

The configuration should be in oauth.conf file that should be in the conf directory.

These are the configuration options

|Property  |Required   |Description|
|----------|-----------|------------|
|auth.oauth.jwksURL     |Required       |The JWKS URL to validate the Token. Ex: https://login.microsoftonline.com/common/discovery/keys        |
|auth.oauth.expectedAudience     |Required       |The Expected Audience for this token. Ex: https://storage.azure.com        |
|auth.oauth.expectedIssuer     |Required       |The Expected issuer of this Token. Ex: https://sts.windows.net/54e85725-ed2a-49a4-a19e-11c8d29f9a0f/        |
|auth.oauth.useGroupMapping     |Optional       |Should Group Mapping configuration be used.        |
|auth.oauth.groupsMapping|Optional   |Conf file that can be used to map the group claims to neo4J roles. The file should be int he conf directory.|
|auth.oauth.userName     |Optional       |The user name claim name that needs to be to used for principal field. Default value: unique_name       |
|auth.oauth.groupsClaim     |Optional       |The groups claims name. Default value: groups      |

# Group Mapping
This is optional configuration. The file should have this format (Java Properties)

    group_name=role
