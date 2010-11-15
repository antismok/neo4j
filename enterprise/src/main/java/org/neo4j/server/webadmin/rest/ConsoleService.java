/**
 * Copyright (c) 2002-2010 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.neo4j.server.webadmin.rest;

import org.apache.log4j.Logger;
import org.neo4j.server.database.Database;
import org.neo4j.server.rest.domain.JsonHelper;
import org.neo4j.server.webadmin.console.ConsoleSession;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
import java.util.List;
import java.util.Map;

import static org.neo4j.server.rest.domain.JsonHelper.jsonToMap;

@Path( ConsoleService.SERVICE_PATH )
public class ConsoleService implements AdvertisableService
{

    private static final String SERVICE_NAME = "console";
    static final String SERVICE_PATH = "server/console";
    private ConsoleSession session;

    public ConsoleService( ConsoleSession session )
    {
        this.session = session;
    }

    public ConsoleService( @Context Database database,
                           @Context HttpServletRequest req )
    {
        this( createSession( req, database ) );
    }

    private static ConsoleSession createSession( HttpServletRequest req,
                                                 Database database )
    {
        HttpSession httpSession = req.getSession( true );
        Object session = httpSession.getAttribute( "consoleSession" );
        if ( session == null )
        {
            session = new ConsoleSession( database );
            httpSession.setAttribute( "consoleSession", session );
        }
        return (ConsoleSession)session;
    }

    Logger log = Logger.getLogger( ConsoleService.class );

    public String getName()
    {
        return SERVICE_NAME;
    }

    public String getServerPath()
    {
        return SERVICE_PATH;
    }

    @GET
    @Produces( MediaType.APPLICATION_JSON )
    public Response getServiceDefinition( @Context UriInfo uriInfo )
    {
        return Response.ok( "" ).header( "Content-Type", MediaType.APPLICATION_JSON ).build();
    }

    @POST
    @Produces( MediaType.APPLICATION_JSON )
    @Consumes( MediaType.APPLICATION_FORM_URLENCODED )
    public Response formExec( @FormParam( "value" ) String data )
    {
        return exec( data );
    }

    @POST
    @Produces( MediaType.APPLICATION_JSON )
    @Consumes( MediaType.APPLICATION_JSON )
    public Response exec( String data )
    {
        try
        {
            Map<String, Object> args = jsonToMap( data );

            if ( !args.containsKey( "command" ) )
            {
                throw new IllegalArgumentException( "Missing 'command' parameter in arguments." );
            }

            log.info( session.toString() );

            List<String> resultLines = session.evaluate((String) args.get("command"));

            return Response.ok( JsonHelper.createJsonFrom( resultLines ) ).header( "Content-Type", MediaType.APPLICATION_JSON ).build();
        } catch ( IllegalArgumentException e )
        {
            return Response.status( Status.BAD_REQUEST ).build();
        }
    }

}
