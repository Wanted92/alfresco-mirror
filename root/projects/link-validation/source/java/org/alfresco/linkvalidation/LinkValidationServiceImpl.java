/*-----------------------------------------------------------------------------
*  Copyright 2007 Alfresco Inc.
*  
*  This program is free software; you can redistribute it and/or modify
*  it under the terms of the GNU General Public License as published by
*  the Free Software Foundation; either version 2 of the License, or
*  (at your option) any later version.
*  
*  This program is distributed in the hope that it will be useful, but
*  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
*  or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
*  for more details.
*  
*  You should have received a copy of the GNU General Public License along
*  with this program; if not, write to the Free Software Foundation, Inc.,
*  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.  As a special
*  exception to the terms and conditions of version 2.0 of the GPL, you may
*  redistribute this Program in connection with Free/Libre and Open Source
*  Software ("FLOSS") applications as described in Alfresco's FLOSS exception.
*  You should have received a copy of the text describing the FLOSS exception,
*  and it is also available here:   http://www.alfresco.com/legal/licensing
*  
*  
*  Author  Jon Cox  <jcox@alfresco.com>
*  File    LinkValidationServiceImpl.java
*----------------------------------------------------------------------------*/

package org.alfresco.linkvalidation;

import java.io.File;

import java.io.IOException;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.net.ssl.SSLException;
import org.alfresco.config.JNDIConstants;
import org.alfresco.filter.CacheControlFilter;
import org.alfresco.mbeans.VirtServerRegistry;
import org.alfresco.repo.attributes.Attribute;
import org.alfresco.repo.attributes.BooleanAttribute;
import org.alfresco.repo.attributes.BooleanAttributeValue;
import org.alfresco.repo.attributes.IntAttribute;
import org.alfresco.repo.attributes.IntAttributeValue;
import org.alfresco.repo.attributes.MapAttribute;
import org.alfresco.repo.attributes.MapAttributeValue;
import org.alfresco.repo.attributes.StringAttribute;
import org.alfresco.repo.attributes.StringAttributeValue;
import org.alfresco.repo.domain.PropertyValue;
import org.alfresco.sandbox.SandboxConstants;
import org.alfresco.service.cmr.attributes.AttrAndQuery;
import org.alfresco.service.cmr.attributes.AttributeService;
import org.alfresco.service.cmr.attributes.AttrQueryGTE;
import org.alfresco.service.cmr.attributes.AttrQueryLTE;
import org.alfresco.service.cmr.avm.AVMNodeDescriptor;
import org.alfresco.service.cmr.avm.AVMNotFoundException;
import org.alfresco.service.cmr.avmsync.AVMDifference;
import org.alfresco.service.cmr.avmsync.AVMSyncService;
import org.alfresco.service.cmr.remote.AVMRemote;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.MD5;
import org.alfresco.util.NameMatcher;
import org.alfresco.util.Pair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**

Here's a sketch of the algorithm

<pre>

 Before starting, create an empty url cache for this changeset update.
 This will allow us to skip certain redundant ops like status checks.

 For each file F, calulate a url U.

  [1]   Given a source file, what hrefs appear in it explicitly/implicitly:
            md5_source_to_md5_href[ md5( file ) ]  --> map { md5( url ) }
            .href/mysite/|mywebapp/-2/md5_source_to_md5_href/

  [2]   Given an href, in what source files does it appear 
        explicitly or implicitly (via dead reconing):
            md5_href_to_md5_source[ md5( url ) ]  --> map { md5( file ) }
            .href/mysite/|mywebapp/-2/md5_href_to_source

  [3]   Given an href, what's its status?
            md5_href_to_status[   md5( url ) ]  --> 200/404, etc.
            .href/mysite/|mywebapp/-2/md5_href_to_status/

  [4]   Given a status, what hrefs have it?
            status_to_md5_href[   status  ]  --> map { md5( url ) }
            .href/mysite/|mywebapp/-2/status_to_md5_href/

  [5]   Given an md5, what's the filename?
            md5_to_file[          md5( file ) ]  --> String( file )
            .href/mysite/|mywebapp/-2/md5_to_file/
        
  [6]   Given an md5, what's the href?
            md5_to_href[          md5( url ) ]  --> String( url )
            .href/mysite/|mywebapp/-2/md5_to_href/

  [7]   Given an href what files does it depend on?
             hamd5_href_to_md5_fdep[ md5( url ) ]  --> map { md5( file ) } 
            .href/mysite/|mywebapp/-2/md5_href_to_md5_fdep/

  [8]   Given a file, what hrefs depend on it
            md5_file_to_md5_hdep[ md5( file ) ]  --> map { md5( url ) }     
            .href/mysite/|mywebapp/-2/file_to_hdep/

  On file creation Fj
  --------------------
  [c0]  Calculate href Ui for Fj, and enter md5(Ui) into ephemeral url cache
  [c1]  Pull on Ui, get file set accessed F 
        This is usually just Fj but could be Fj,...Fk).
            If F = {} or does not include Fj either we failed to compute 
            Ui from Fj or an error has occured when we pulled on Ui.  
            skip all remaining work on Fj & track the error (possibly
            report to gui or just log).  This is a "soft" failure because
            the link's existence is implied by the file's existence 
            (we didn't *actually* extract it from a returned html page).

  [c2]  Update [1] with md5(Fj) -> md5(Ui)  (handles implicit link)
  [c3]  Update [2] with md5(Ui) -> md5(Fj)  (handles implicit link)
  [c4]  Update [3] with md5(Ui) -> status
  [c5]  Update [4] with status  -> md5(Ui)
  [c6]  Update [5] with md5(Fj) -> Fj,       md5(Fk) -> Fk,  ...
  [c7]  Update [6] with md5(Ui) -> Ui
  [c8]  Update [7] with md5(Ui) -> md5(Fj),  md5(Ui) -> md5(Fk), ...
  [c9]  Update [8] with md5(Fj) -> md5(Ui),  md5(Fk) -> md5(Ui), ... 

        For every url Ux in the page (but not the dead-reconed one Ui),
        If Ux is already in the ephemeral url cache,
  [c10]     Next Ux
        Else
  [c11]     Pull on Ux and regardless of what happens:
  [c12]     Update [1] with md5(Fj) -> md5(Ux)
  [c13]     Update [2] with md5(Ux) -> md5(Fj)
  [c14]     Update [3] with md5(Ux) -> status
  [c15]     Update [4] with status  -> md5(Ux)
  [c16] 
        If status==success, determine which files are accessed Fx,Fy, ...
  [c17]     Update [5] with md5(Fx) -> Fx,  md5(Fy) -> Fy, ...
  [c18]     Update [6] with md5(Ux) -> Ux
  [c19]     Update [7] with md5(Ux) -> Fx,  md5(Ux) -> Fy, ...
  [c20]     Update [8] with md5(Fx) -> md5(Ux), md5(Fy) -> md5(Ux), ...


  On file modification
  --------------------

  [m0] Calculate href Ui for Fj, if already in ephermal cache, do next Fj
  [m1] == [c1]
  [m2] == [c2] but it's a no-op
  [m3] == [c3] but it's a no-op
  [m4] == [c4]
  [m5] == [c5]
  [m6] == [c6] but it's a no-op for md5(Fj)
  [m7] == [c7] but it's a no-op
  [m8] == [c8] but it's a no-op for md5(Ui) -> md5(Fj)
  [m9] == [c9] but it's a no-op for md5(Fj) -> md5(Ui)

  [m10]  Parse & get list of hrefs curently in page, plus link to self: Ucurr
  [m11]  Using [1], get previous href list:    Uprev
  [m12]  Subtracing lists, find urls now gone: Ugone
         Note:  implicit link to self never 
                appears in Ugone on modifiction
                becuse [m10] includes implicit 
                link to self.  
        
         For each unique URL Ux in Ugone (no cache check):
  [m13]         Update [1], removing  md5(Fj) -> md5(Ux)
  [m14]         Update [2], removing  md5(Ux) -> md5(Fj)
  [m15]         If [2] shows that href no longer appears anywhere, then:
  [m16]              Remove from [2]  md5(Ux)
  [m17]              Remove from [6]  md5(Ux) -> Ux
  [m18]              Using [7], 
                     for each file md5(Fx) depending on the defunct md5(Ux):
                          Remove from [8]  md5(Fx) -> md5(Ux)
  [m19]              Remove from [7] md5(Ux)
  [m20]              Using [3], fetch status of Ux. If there's a status 
                     then:
  [m21]                      Remove from [3]  md5(Ux) -> status
  [m22]                      Remove from [4]  status  -> md5(Ux)

         For each unique URL Ux in Ucurr
         If Ux isn't already in the ephermal url cache, do:
  [m24..m34] ==  [c10..20]



  On proposed file deletion
  -------------------------
   [p0]  Use [8] to get raw list of URLs that depend on Fj.

         For each URL Ux in [8]:
   [p1]     Use [2] to get list of files with newly broken links
            but omit from this list the Fj itself
            (because it's going away).


  On file deletion Fj
  --------------------
  [d0]  Use [1] to get list of hrefs that appear in Fj explicitly 
        and implicitly.  Call this set Ugone.  

        For each URL Ux in Ugone
  [d1-d9] == [m14..m22] 

  [d10-d11] == [p0-p1]

  [d12]  For each broken link discovered in [d10-d11], update [3] and [4] 
         with 404 status (assumed)... or perhaps some other 4xx status
         to denote "presumed broken" if you don't really test it.

  [d13]  Update [1] by removing md5( Fj )   (remove all urls in one shot)

  [d14]  Using [8], if nothing depends on md5(Fj), 
         then remove md5(Fj) from [5] and [8].

</pre>
*/


public class LinkValidationServiceImpl implements LinkValidationService,
                                                  Runnable
{
    private static Log log = LogFactory.getLog(LinkValidationServiceImpl.class);

    // Shutdown flag for service
    private static AtomicBoolean Shutdown_ = new AtomicBoolean( false );

    static String HREF                 = ".href";    // top level href key

    static String LATEST_VERSION       = "latest";   // numerical version
    static String LATEST_VERSION_ALIAS = "-2";       // alias for numerical

    static String SOURCE_TO_HREF       = "source_to_href";  // key->map
    static String HREF_TO_SOURCE       = "href_to_source";  // key->map

    static String HREF_TO_STATUS       = "href_to_status";  // key->int
    static String STATUS_TO_HREF       = "status_to_href";  // key->map

    static String MD5_TO_FILE          = "md5_to_file";     // key->string
    static String MD5_TO_HREF          = "md5_to_href";     // key->string

    
    // All files on which a hyperlink depends
    static String HREF_TO_FDEP         = "href_to_fdep";    // key->map

    // All hyperinks dependent upon a given file
    static String FILE_TO_HDEP         = "file_to_hdep";    // key->map


    AVMRemote          avm_;
    AttributeService   attr_;
    AVMSyncService     sync_;
    NameMatcher        excluder_;


    VirtServerRegistry virtreg_;

    public LinkValidationServiceImpl() { }

    public void setAttributeService(AttributeService svc) { attr_ = svc; }
    public AttributeService getAttributeService()         { return attr_;}

    public void setAVMSyncService( AVMSyncService sync )  { sync_ = sync;}
    public AVMSyncService getAVMSyncService()             { return sync_;}

    public void setAvmRemote(AVMRemote svc)               { avm_ = svc; }
    public AVMRemote getAvmRemote()                       { return avm_;}

    public void setExcludeMatcher(NameMatcher matcher)  { excluder_ = matcher;}
    public NameMatcher getExcludeMatcher()              { return excluder_;}

    public void setVirtServerRegistry(VirtServerRegistry reg) {virtreg_ = reg;}
    public VirtServerRegistry getVirtServerRegistry()         {return virtreg_;}

    //-------------------------------------------------------------------------
    /**
    *  Called by LinkValidationServiceBootstrap at startup time to ensure 
    *  that the link status in all stores is up to date.
    */
    //-------------------------------------------------------------------------
    public void onBootstrap()
    {
        Thread validation_update_thread = new Thread(this);
        Shutdown_.set( false );    
        validation_update_thread.start();
    }
    //-------------------------------------------------------------------------
    /**
    *  Called by LinkValidationServiceBootstrap at shutdown time to ensure 
    *  that any link status checking operation in progress is abandoned.
    */
    //-------------------------------------------------------------------------
    public void onShutdown() { Shutdown_.set( true ); }


    //-------------------------------------------------------------------------
    /**
    *   Main thread to update href validation info in staging.
    */
    //-------------------------------------------------------------------------
    public void run()
    {
        // Initiate background process to check links
        // For now, hard-code initial update
        String   webappPath       = null;    // all stores/webapps
        boolean  incremental      = true;    // use deltas & merge
        boolean  validateExternal = true;    // check external hrefs
        int      connectTimeout   = 10000;   // 10 sec
        int      readTimeout      = 30000;   // 30 sec
        long     poll_interval    = 2000;    // 2 sec
        int      nthreads         = 5;       // ignored

        HrefValidationProgress progress = null;;

        while ( ! Shutdown_.get() ) 
        {
            progress = new HrefValidationProgress();
            try 
            {
                updateHrefInfo( webappPath,
                                incremental,
                                validateExternal,
                                readTimeout,
                                connectTimeout,
                                nthreads,
                                progress);

                Thread.sleep( poll_interval );
            }
            catch (Exception e) { /* nothing to do */ }
        }

        // Usually, tomcat will be dead by now, but just in case...

        if ( progress != null) { progress.abort(); }
    }



    //-------------------------------------------------------------------------
    /**
    *   
    * @param  webappPath
    *           Path to webapp
    *
    * @param  incremental
    *           Use deltas if true (faster); otherwise, force
    *           the href info to be updated from scratch. 
    *
    * @param  validateExternal
    *           Validate external links 
    *
    * @param connectTimeout  
    *           Amount of time in milliseconds that this function will wait
    *           before declaring that the connection has failed 
    *           (e.g.: 10000 ms).
    *
    * @param readTimeout     
    *           time in milliseconds that this function will wait before
    *           declaring that a read on the connection has failed
    *           (e.g.:  30000 ms).
    * 
    * @param nthreads
    *             Number of threads to use when fetching URLs (e.g.: 5)
    *
    * @param  progress           
    *             While updateHrefInfo() is a synchronous function, 
    *             'progress' may be polled in a separate thread to 
    *             observe its progress.
    */
    //-------------------------------------------------------------------------
    public void updateHrefInfo( String                 webappPath,
                                boolean                incremental,
                                boolean                validateExternal,
                                int                    connectTimeout,
                                int                    readTimeout,
                                int                    nthreads,
                                HrefValidationProgress progress)          
                throws          AVMNotFoundException,
                                SocketException,
                                SSLException,
                                LinkValidationAbortedException
    {
        // RESUME HERE
        //  Handle update to staging
        //  If webappPath == null, do all stores
        //  if webappPath == a store do all webapps in that store
        //  Handle each weapp separately in a different try block
        //  if given a null or store instead of a webapp
    }


    //------------------------------------------------------------------------
    //------------------------------------------------------------------------
    /**
    *  Computes what changes would take place the link status of staging
    *  when diffs from a srcWebappPath are applied to staging.  There are
    *  four state transitions of interest:
    *
    * <pre>
    *          [1]  Broken by deleted files/dirs in src:
    *               o  Any url that both depends on a deleted file and would
    *                  still remain in a file after the set was deleted
    *                 -  Scales with # deletions
    *                 -  Computable via inference (fast)
    *
    *          [2]  Broken by added files/dirs in src:
    *               o  Any url that is broken in the changeset
    *                 - Scales with # of links in changeset
    *                 - Potentially slow
    *
    *
    *          [3]  Fixed by deleted files/dirs in src:
    *               o  Any url that is broken and only occurs in the
    *                  members of the set being deleted
    *                  -  Scales with # deletions
    *                  -  Computable via inference (fast)
    *
    *          [4]  Fixed by added files/dirs in src:
    *               o  Any url that was broken that is now fixed
    *                  Must rescan all broken links still present
    *                  in changeset to be 100% sure.
    *                  - Scales with the # of broken links in staging
    *                  - Potentially slow
    *
    * </pre>
    */
    public HrefDifference getHrefDifference( 
                              String  srcWebappPath, 
                              String  dstWebappPath, 
                              int     connectTimeout,
                              int     readTimeout,
                              int     nthreads,    // NEON - ignored for now
                              HrefValidationProgress progress)   
            throws            AVMNotFoundException,
                              SocketException,
                              SSLException,
                              LinkValidationAbortedException
    {
        // TODO:  Make this work with latest snapshot instead of HEAD
        int srcVersion = -1;
        int dstVersion = -1;

        if ( progress != null ) 
        { 
            progress.init(); 
        }
        
        try
        {
           return  getHrefDifference( srcVersion,     
                                      srcWebappPath,
                                      dstVersion,
                                      dstWebappPath,
                                      connectTimeout, 
                                      readTimeout,
                                      nthreads,
                                      progress);
        }
        finally
        {
            if (progress != null)  
            {
                progress.setDone( true ); 
            }
        }
    }


    /*-----------------------------------------------------------------------*/
    /**
    * Determines how hrefs have changed between two stores,
    * along with status info, and returns the result;
    * this href difference info is not merged with the 
    * overall href status info for the webapp. 
    *
    * Thus, you can use this function to see how the status of hyperlinks 
    * would change if you were to incorporate the diff without actually 
    * incorporating it.  After calling this function, the user can see 
    * what's broken by invoking:
    * <ul>
    *   <li>  getHrefManifestBrokenByDeletion()
    *   <li>  getHrefManifestBrokenInNewOrMod()
    * </ul>
    */
    /*-----------------------------------------------------------------------*/
    public HrefDifference getHrefDifference( 
                                  int    srcVersion,
                                  String srcWebappPath,
                                  int    dstVersion,
                                  String dstWebappPath,
                                  int    connectTimeout,
                                  int    readTimeout,
                                  int    nthreads,
                                  HrefValidationProgress progress)
                          throws  AVMNotFoundException,
                                  SocketException,
                                  SSLException,
                                  LinkValidationAbortedException
    {
        ValidationPathParser dp = new ValidationPathParser(avm_,dstWebappPath);
        String dst_dns_name     = dp.getDnsName();
        String dst_req_path     = dp.getRequestPath();
        String webapp_name      = dp.getWebappName();

        if (webapp_name == null ) 
        { 
            throw new RuntimeException( "Not a path to a webapp: " + 
                                        dstWebappPath);
        }

        List<AVMDifference> diffs = sync_.compare( srcVersion, 
                                                   srcWebappPath, 
                                                   dstVersion, 
                                                   dstWebappPath, 
                                                   excluder_);
            
        ValidationPathParser sp = new ValidationPathParser(avm_, srcWebappPath);
        String src_dns_name     = sp.getDnsName();
        String src_req_path     = sp.getRequestPath();

        String virt_domain      = virtreg_.getVirtServerFQDN();
        int    virt_port        = virtreg_.getVirtServerHttpPort();

        String src_fqdn         = src_dns_name + ".www--sandbox.version--v" + 
                                  srcVersion   + "." + virt_domain;

        String dst_fqdn         = dst_dns_name + ".www--sandbox.version--v" + 
                                  dstVersion   + "." + virt_domain;


        String src_webapp_url_base = null;
        String dst_webapp_url_base = null;
        try 
        {
            URI u;

            u = new URI( "http",       // scheme
                         null,         // userinfo
                         src_fqdn,     // host
                         virt_port,    // port
                         src_req_path, // request path
                         null,         // query
                         null);        // frag

            // http://alice.mysite.www--sandbox.
            //       version--v-1.127-0-0-1.ip.alfrescodemo.net:8180/

            src_webapp_url_base = u.toASCIIString();

            u = new URI( "http",       // scheme
                         null,         // userinfo
                         dst_fqdn,     // host
                         virt_port,    // port
                         dst_req_path, // request path
                         null,         // query
                         null);        // frag

            // http://mysite.www--sandbox.
            //        version--v-1.127-0-0-1.ip.alfrescodemo.net:8180/

            dst_webapp_url_base = u.toASCIIString();
        }
        catch (Exception e) { /* can't happen */ }

        
        // Examine the diffs between src and dst
        //
        //    o  If a file or dir is non-deleted, then fetch all the 
        //       relevant link from src and store the data collected
        //       in href_manifest and href_status_map.
        //    
        //    o  If a file or dir is deleted, then update the
        //       deleted_file_md5 and the broken_hdep cache from dst.

        String store_attr_base  = getAttributeStemForDnsName(dst_dns_name,
                                                             false,
                                                             true);
        String href_attr        = store_attr_base    + 
                                  "/|" + webapp_name +
                                  "/"  + LATEST_VERSION_ALIAS;

        HrefDifference href_diff = new HrefDifference(href_attr, 
                                                      sp.getStore(),
                                                      dp.getStore(),
                                                      src_webapp_url_base,
                                                      dst_webapp_url_base,
                                                      connectTimeout,
                                                      readTimeout,
                                                      nthreads);

        HrefStatusMap      href_status_map  = href_diff.getHrefStatusMap();
        HrefManifest       href_manifest    = href_diff.getHrefManifest();
        Map<String,String> deleted_file_md5 = href_diff.getDeletedFileMd5();



        HashMap<String,String> broken_hdep_cache = new HashMap<String,String>();
        MD5 md5 = new MD5();

        for (AVMDifference diff : diffs )
        {
            String src_path  = diff.getSourcePath();

            // Look up source node, even if it's been deleted
            
            AVMNodeDescriptor src_desc = avm_.lookup(srcVersion,src_path,true);
            if (src_desc == null ) { continue; }

            if (src_desc.isDeleted() )
            {
                // Deal with deleted files

                String dst_path  = diff.getDestinationPath();
                
                if ( src_desc.isDeletedDirectory() )            // deleted dir
                {                                              
                    update_dir_gone_broken_hdep_cache(
                        dstVersion, 
                        dst_path,
                        deleted_file_md5,
                        broken_hdep_cache,
                        href_attr,
                        md5,
                        progress);

                    // stats for monitoring
                    if ( progress != null ) 
                    { 
                        progress.incrementDirUpdateCount(); 
                    }
                }
                else                                            // deleted file
                {
                    update_file_gone_broken_hdep_cache( 
                        dst_path,
                        deleted_file_md5,
                        broken_hdep_cache,
                        href_attr,
                        md5,
                        progress);

                    // stats for monitoring
                    if ( progress != null )
                    {
                        progress.incrementFileUpdateCount();
                    }
                }
            }
            else
            {
                // New, modified, or conflicted  files/dirs

                ValidationPathParser spath = 
                        new ValidationPathParser(avm_, src_path);

                String req_path = spath.getRequestPath();

                if (src_desc.isDirectory())
                {
                    // recurse within src for new links
                    extract_links_from_dir( 
                            srcVersion,
                            src_path,
                            src_fqdn,
                            virt_port,
                            req_path,
                            href_manifest,       // sync list
                            href_status_map,     // sync map
                            connectTimeout,
                            readTimeout,
                            progress,
                            0);

                    // stats for monitoring
                    if ( progress != null )
                    {
                        progress.incrementDirUpdateCount();
                    }
                }
                else
                {
                    // Get links from src_path and status of implicit 
                    // (dead-reckoned) link to it, but don't pull
                    // on the parsed links yet.

                    extract_links_from_file( 
                            src_path,
                            src_fqdn,
                            virt_port,
                            req_path,
                            href_manifest,       // sync list
                            href_status_map,     // sync map
                            connectTimeout,
                            readTimeout,
                            progress);

                    // stats for monitoring
                    if (progress != null)
                    {
                        progress.incrementFileUpdateCount();
                    }
                }
            }
        }


        // TODO:  When extract_links_from_file is multi-threaded,
        //        put a thread barrier here, so that all the dead
        //        reckoned links have been validated before proceeding.
        //
        

        // Get status of links in href_manifest if not already known 
        List<HrefManifestEntry> manifest_entry_list =  
                href_manifest.getManifestEntries();

        for ( HrefManifestEntry manifest_entry : manifest_entry_list )
        {
            List<String> href_list = manifest_entry.getHrefs();
            for (String parsed_url : href_list)
            {
                if ( href_status_map.get( parsed_url ) == null )
                {
                    validate_href( parsed_url,
                                   href_status_map,
                                   parsed_url.startsWith( src_webapp_url_base ),
                                   false,
                                   connectTimeout,
                                   readTimeout,
                                   progress);
                }
            }
        }

        // TODO:  When validating url for cache is multi-threaded
        //        put a thread barrier here so that we've got 
        //        status info for all parsed links after this point


        // Remove from the collection of "broken" hyperlinks 
        // anything that is no longer referenced by any file
        //
        // Compute what's broken by the deletion
        
        Map<String, List<String>> broken_manifest_map = 
                href_diff.getBrokenManifestMap();


        for ( String broken_href_md5 : broken_hdep_cache.keySet() )
        {
            Set<String> file_md5_set = 
                    attr_.getAttribute( href_attr      + "/" + 
                                        HREF_TO_SOURCE + "/" + 
                                        broken_href_md5
                                      ).keySet();

            ArrayList<String> conc_file_list = new ArrayList<String>();
            
            for ( String file_md5 : file_md5_set )
            {
                if  (  ! deleted_file_md5.containsKey(  file_md5 ) )
                {
                    String conc_file = 
                       attr_.getAttribute( href_attr   + "/" + 
                                           MD5_TO_FILE + "/" + 
                                           file_md5
                                         ).getStringValue();
                    
                    conc_file_list.add( conc_file );
                }
            }
            
            String broken_href = attr_.getAttribute( href_attr   + "/" +
                                                     MD5_TO_HREF + "/" +
                                                     broken_href_md5
                                                   ).getStringValue();

            // This broken href is relevant because it still exists in some file
            if ( conc_file_list.size() > 0 )
            {
                for ( String broken_file : conc_file_list )
                {
                    List<String> manifest_list = 
                        broken_manifest_map.get(broken_file);         

                    if ( manifest_list == null )
                    {
                        manifest_list = new ArrayList<String>();
                        broken_manifest_map.put( broken_file, manifest_list);
                    }
                    manifest_list.add( broken_href );
                }
            }
        }

        // Now to see what's broken in the update, the client can call:
        // 
        //    getHrefManifestBrokenByDeletion()
        //    getHrefManifestBrokenInNewOrMod()

        return href_diff;
    }


    /*-------------------------------------------------------------------------
    *  getHrefManifestBrokenByDelete --
    *        
    *------------------------------------------------------------------------*/
    public HrefManifest getHrefManifestBrokenByDelete(HrefDifference href_diff)
    {
        if ( href_diff.broken_by_deletion_ != null ) 
        { 
            return href_diff.broken_by_deletion_;
        }

        href_diff.broken_by_deletion_ = new HrefManifest();


        Map<String, List<String>> broken_manifest_map = 
                href_diff.getBrokenManifestMap();

        ArrayList<String> broken_file_list = 
            new ArrayList<String>( broken_manifest_map.keySet() );

        Collections.sort( broken_file_list);

        // push result into href_diff 

        for (String broken_file : broken_file_list)
        {
            List<String> broken_href_list =
                broken_manifest_map.get(broken_file);

            Collections.sort( broken_href_list );

            href_diff.broken_by_deletion_.add(
                new HrefManifestEntry( broken_file,  broken_href_list ));
        }
        return href_diff.broken_by_deletion_;
    }


    /*-------------------------------------------------------------------------
    *  getHrefManifestBrokenByNewOrMod --
    *        
    *------------------------------------------------------------------------*/
    public HrefManifest getHrefManifestBrokenByNewOrMod(
                            HrefDifference href_diff)
    {
        if ( href_diff.broken_in_newmod_ != null)   // If already calculated
        {                                           // then just return the
            return href_diff.broken_in_newmod_;     // old result
        }

        // Derived a pruned version of the href_manifest to get only those
        // new/modified files with at least one broken link.  Note that
        // href_diff.broken_in_newmod_ is a subset of href_diff.href_manifest_
        // in both files mentioned and hrefs per file.

        href_diff.broken_in_newmod_ = new HrefManifest();   

        List<HrefManifestEntry> manifest_entry_list =  
                href_diff.href_manifest_.getManifestEntries();

        HrefStatusMap href_status_map  = href_diff.getHrefStatusMap();

        for ( HrefManifestEntry manifest_entry : manifest_entry_list )
        {
            ArrayList<String> broken_href_list = new ArrayList<String>();
            List<String>      href_list        = manifest_entry.getHrefs();

            for (String parsed_url : href_list)
            {
                int status_code = href_status_map.get(parsed_url).getFirst();

                if (status_code >= 400 )
                {
                    broken_href_list.add( parsed_url );
                }
            }

            if  ( broken_href_list.size() > 0 )
            {
                href_diff.broken_in_newmod_.add( 
                   new HrefManifestEntry( manifest_entry.getFileName(),
                                          broken_href_list));
            }
        }
        return href_diff.broken_in_newmod_;
    }

    //-------------------------------------------------------------------------
    /**
    * Walk the list of files that are new or modified (newmod_file)
    * and update the following parameters:
    * <pre>
    *
    *   newmod_conc           Contains as keys any new or modified dst_url_md5.
    *                         Its values are a map keys of dst_file_md5
    *                         (that have null values).
    *                         
    *   deleted_conc          Contains as keys any deleted dst_url_md5.
    *                         its values are a map of the dst_file_md5 
    *                         (that have null values).
    *                         
    *   newmod_file           Contains as keys any new or modified dst_file_md5.
    *                         Its values are the associated dst_file
    *                         
    *   href_in_conc          Contains as keys dst_url_md5 that is present
    *                         in either newmod_conc or deleted_conc.  Its
    *                         values are the associated dst_url.  When the
    *                         href appears in newmod_conc but not deleted_conc,
    *                         its value in is the actual url, not null.
    *                         This serves as a flag that allows some calls 
    *                         to set md5 -> url to be skipped.
    *
    *   newmod_manifest_list  Like the manifest_list, only all values are
    *                         the md5sum of their translation into the dst
    *                         namespace.
    *
    * </pre>
    */
    //-------------------------------------------------------------------------
    void build_changeset_concordances(
            HrefDifference                       href_diff,
            Map<String, HashMap<String,String>>  newmod_conc,
            Map<String, HashMap<String,String>>  deleted_conc,
            Map<String,String>                   newmod_file,
            Map<String,String>                   href_in_conc,
            List<HrefManifestEntry>              newmod_manifest_list,
            String                               dst_webapp_url_base,
            String                               src_webapp_url_base,
            int                                  src_webapp_url_base_length,
            int                                  src_store_length,
            String                               dst_store,
            MD5                                  md5)
    {
        String                  href_attr;
        List<HrefManifestEntry> manifest_entry_list;

        href_attr = href_diff.getHrefAttr();
        manifest_entry_list =  href_diff.getHrefManifest().getManifestEntries();

        // 
        // Build concordance of new or modified files
        //

        for ( HrefManifestEntry manifest_entry : manifest_entry_list )
        {
            String newmod_src_file = manifest_entry.getFileName();
            String newmod_dst_file = 
                      dst_store + newmod_src_file.substring( src_store_length );

            String newmod_dst_file_md5 = md5.digest(newmod_dst_file.getBytes());
            newmod_file.put( newmod_dst_file_md5, newmod_dst_file );

            List<String> href_list = manifest_entry.getHrefs();
            ArrayList<String> src_dst_href_md5_list = new ArrayList<String>();
             
            for (String src_url : href_list)
            {
                String dst_url;
                if ( ! src_url.startsWith( src_webapp_url_base ) )
                {
                    dst_url = src_url;
                }
                else
                {
                    dst_url = dst_webapp_url_base +
                              src_url.substring( src_webapp_url_base_length );
                }

                String dst_url_md5 = md5.digest( dst_url.getBytes() );

                src_dst_href_md5_list.add( dst_url_md5 );
                
                HashMap<String,String> url_locations =  
                  newmod_conc.get( dst_url_md5 );

                // href_in_conc has as keys any href in either
                // newmod_conc or deleted_conc.  Note that because
                // the url could be new, its value is set to dst_url 
                // rather than null.

                href_in_conc.put( dst_url_md5, dst_url );

                if (  url_locations == null )
                {
                    url_locations = new HashMap<String,String>();
                    newmod_conc.put( dst_url_md5, url_locations);
                }
                url_locations.put( newmod_dst_file_md5, null );
            }
            newmod_manifest_list.add(
                new HrefManifestEntry( newmod_dst_file_md5, 
                                       src_dst_href_md5_list));
        }


        // 
        // Build concordance of deleted files
        //

        Map<String,String>  deleted_file_md5_map;
        deleted_file_md5_map = href_diff.getDeletedFileMd5();

        for (String deleted_file_md5 : deleted_file_md5_map.keySet() )
        {
            // Get the set of hrefs that are now gone from deleted file
        
            Set<String> deleted_href_md5_set =
                    attr_.getAttribute( href_attr      + "/" + 
                                        SOURCE_TO_HREF + "/" + 
                                        deleted_file_md5
                                      ).keySet();

            for ( String deleted_href_md5 : deleted_href_md5_set )
            {
                // href_in_conc contains as keys any hrefs 
                // present in newmod_conc or deleted_conc. 
                //
                // Because the href is deleted, there's no need to worry 
                // about the  md5->href mapping, so just use null
                // to allow some md5 -> href updates to be skipped.

                href_in_conc.put( deleted_href_md5, null );

                HashMap<String,String> url_locations =  
                        deleted_conc.get( deleted_file_md5 );

                if (url_locations == null )
                {
                    url_locations = new HashMap<String,String>();
                    deleted_conc.put( deleted_href_md5, url_locations );
                }
                url_locations.put(deleted_file_md5, null );
            }
        }
    }

    /*-------------------------------------------------------------------------
    *  merge_href_to_source_and_source_to_href --
    *       Merges HREF_TO_SOURCE and new SOURCE_TO_HREF data,
    *       and gets rid of stale hrefs.   Does not get rid
    *       of stale SOURCE_TO_HREF data (that's handled elsewhere).
    *------------------------------------------------------------------------*/
    void merge_href_to_source_and_source_to_href( 
                 HrefDifference                       href_diff,
                 Map<String, HashMap<String,String>>  newmod_conc,
                 Map<String, HashMap<String,String>>  deleted_conc,
                 Map<String,String>                   newmod_file,
                 Map<String,String>                   href_in_conc,
                 List<HrefManifestEntry>              newmod_manifest_list)
    {
        String href_attr = href_diff.getHrefAttr();

        // Look at any href in the changeset   (ie: in a new/mod/del file)

        for (String href_md5  :  href_in_conc.keySet() )
        {
            // updated HREF_TO_SOURCE value for href_md5
            MapAttribute new_href_to_source = new MapAttributeValue();

            Set<String> old_href_to_source_set = 
                    attr_.getAttribute( href_attr      + "/" + 
                                        HREF_TO_SOURCE + "/" + 
                                        href_md5
                                      ).keySet();

            HashMap<String,String> deleted_locations =  
                deleted_conc.get( href_md5  );
             
            // Copy filtered list of locations into  new_href_to_source

            if  ( old_href_to_source_set != null )
            {
                for ( String location : old_href_to_source_set )
                {
                    if ( ( deleted_locations != null  && 
                           deleted_locations.containsKey( location )
                         ) || newmod_file.containsKey( location ))
                    {
                        continue;               // filter this location out
                    }
                    new_href_to_source.put( 
                        location, new BooleanAttributeValue( true ));
                }
            }

            // Add to new_href_to_source any values in newmod_conc

            HashMap<String,String> added_locations =  
                newmod_conc.get( href_md5  );

            if  ( added_locations != null )
            {
                for (String location : added_locations.keySet() )
                {
                    new_href_to_source.put( 
                        location, new BooleanAttributeValue( true ));
                }
            }

            // If new_href_to_source is empty, this is now a stale HREF
            if  ( new_href_to_source.size() == 0)
            {
                attr_.removeAttribute( href_attr + "/" + HREF_TO_SOURCE,
                                       href_md5);

                Attribute rsp = 
                    attr_.getAttribute( href_attr      + "/" + 
                                        HREF_TO_STATUS + "/" +
                                        href_md5
                                      );

                boolean is_href_broken = true;
                if (rsp != null)
                {
                    int response_code = rsp.getIntValue();

                    if ( response_code < 400 ) { is_href_broken = false; }

                    attr_.removeAttribute( href_attr + "/" +  HREF_TO_STATUS,
                                           href_md5);

                    attr_.removeAttribute( href_attr + "/" + STATUS_TO_HREF + 
                                           "/" + response_code,
                                           href_md5
                                         );

                    attr_.removeAttribute( href_attr   + "/" + MD5_TO_HREF,
                                           href_md5);


                    Attribute old_fdep_attribute = 
                        attr_.getAttribute( href_attr    + "/" + 
                                            HREF_TO_FDEP + "/" + 
                                            href_md5);

                    attr_.removeAttribute( href_attr  + "/" + HREF_TO_FDEP,
                                           href_md5);


                    if ( old_fdep_attribute != null )
                    {
                        Set<String> old_fdep_set = old_fdep_attribute.keySet();
                        for (String old_fdep : old_fdep_set)
                        {
                            attr_.removeAttribute(href_attr    + "/" + 
                                                  FILE_TO_HDEP + "/" + old_fdep,
                                                  href_md5);
                        }
                    }
                }
            }
            else
            {
                // If possibly a new href, be sure md5 -> href mapping exists
                String orig_href = href_in_conc.get( href_md5 );
                if  ( orig_href != null )
                {
                    attr_.setAttribute( href_attr + "/" + MD5_TO_HREF, 
                                        href_md5,
                                        new StringAttributeValue( orig_href )
                                      );
                }

                attr_.setAttribute( href_attr + "/" + HREF_TO_SOURCE,
                                    href_md5,
                                    new_href_to_source );
            }
        }

        //  Update SOURCE_TO_HREF 
        for ( HrefManifestEntry dst_md5_entry : newmod_manifest_list)
        {
            String dst_file_md5             = dst_md5_entry.getFileName();
            List<String> dst_href_md5_list  = dst_md5_entry.getHrefs();
            MapAttribute new_source_to_href = new MapAttributeValue();

            for ( String dst_href_md5 : dst_href_md5_list)
            {
                new_source_to_href.put( dst_href_md5, 
                                        new BooleanAttributeValue( true ));
            }

            attr_.setAttribute( href_attr + "/" + SOURCE_TO_HREF,
                                dst_file_md5,
                                new_source_to_href);
        }
    }

    void update_href_status( 
             String href_attr,
             HashMap<String,Pair<Integer,HashMap<String,String>>> status_map)
    {
        HashMap<Integer,String> status_cache = new HashMap<Integer,String>();

        for ( String src_dst_url_md5 : status_map.keySet() )
        {
            Pair<Integer,HashMap<String,String>>  src_status = 
                status_map.get( src_dst_url_md5 );

            int  src_response_code  =  src_status.getFirst(); // new status
            HashMap<String,String> src_dst_fdep_md5_map =     // new fdep
                                       src_status.getSecond(); 


            // Get the old status of this href that's in a new/modified file:

            Attribute rsp = attr_.getAttribute( href_attr      + "/" + 
                                                HREF_TO_STATUS + "/" +
                                                src_dst_url_md5
                                              );

            boolean status_already_correct = false;

            if (rsp != null)
            {
                // If this URL had a status value previously, but the status
                // was different from what it is now, remove the old info now.

                int  dst_response_code = rsp.getIntValue();

                if ( dst_response_code != src_response_code )
                {
                    attr_.removeAttribute( href_attr + "/" +  HREF_TO_STATUS,
                                           src_dst_url_md5);


                    attr_.removeAttribute( href_attr + "/" + STATUS_TO_HREF + 
                                           "/" + dst_response_code,
                                           src_dst_url_md5
                                         );
                }
                else                                // The status of this 
                {                                   // href has not changed.
                    status_already_correct = true;  // Therefore, don't bother
                }                                   // doing an update for it.
            }

            if ( ! status_already_correct )
            {
                attr_.setAttribute( href_attr + "/" + HREF_TO_STATUS, 
                                    src_dst_url_md5,
                                    new IntAttributeValue( src_response_code )
                                  );


                if ( ! status_cache.containsKey( src_response_code ) ) 
                {
                    if ( ! attr_.exists( href_attr      + "/" +     // do actual remote
                                         STATUS_TO_HREF + "/" +     // call to see if
                                         src_response_code )        // this status key
                       )                                            // must be created
                    {                                                
                        attr_.setAttribute( href_attr + "/" + STATUS_TO_HREF, 
                                            "" + src_response_code,
                                            new MapAttributeValue()
                                          );
                    }
                    status_cache.put( src_response_code, null );     // never check again
                }

                attr_.setAttribute( 
                        href_attr + "/" + STATUS_TO_HREF + "/" + src_response_code,
                        src_dst_url_md5,
                        new BooleanAttributeValue( true ));
            }



            // If an fdep is in fdep_already_present, there's no need to add it
            //
            HashMap<String,String> fdep_already_present = 
                new HashMap<String,String>();

            Attribute old_fdep_attribute = null;   // If the href had a status,
            if (rsp != null)                       // it might have old fdep;
            {                                      // otherwise, it can't.
                old_fdep_attribute = 
                    attr_.getAttribute( href_attr    + "/" + 
                                        HREF_TO_FDEP + "/" + 
                                        src_dst_url_md5);

                // If the fdep 
                if ( old_fdep_attribute != null )
                {
                    // Note that List<String> src_dst_fdep_md5_map
                    // contains the (possibly null) incoming fdep list 
                    // that's been translated into the dst namespace.
                    //
                    // If these lists are identical 

                    ArrayList<String> stale_fdep_md5_list = 
                        new ArrayList<String>();

                    Set<String> old_fdep_md5_set = old_fdep_attribute.keySet();
                    for (String old_fdep_md5 : old_fdep_md5_set)
                    {
                        if ( src_dst_fdep_md5_map.containsKey( old_fdep_md5 ) )
                        {
                            // no need to add this by hand later
                            fdep_already_present.put( old_fdep_md5, null );
                        }
                        else { stale_fdep_md5_list.add( old_fdep_md5 ); }
                    }

                    // Remove the stale file dependencies
                    for ( String stale_fdep_md5 :  stale_fdep_md5_list)
                    {
                        attr_.removeAttribute( href_attr    + "/" + 
                                               HREF_TO_FDEP + "/" + 
                                               src_dst_url_md5,
                                               stale_fdep_md5);

                        attr_.removeAttribute(href_attr    + "/" + 
                                              FILE_TO_HDEP + "/" + 
                                              stale_fdep_md5,
                                              src_dst_url_md5);
                    }
                }
            }

            // Add new file dependencies, skipping work if possible.
            for ( String src_dst_fdep_md5 : src_dst_fdep_md5_map.keySet())
            {
                if  ( fdep_already_present.containsKey( src_dst_fdep_md5 ) )
                {
                    continue;           // No need to add dependency
                }

                attr_.setAttribute(  href_attr + "/" + HREF_TO_FDEP,
                                     src_dst_url_md5, 
                                     new StringAttributeValue(src_dst_fdep_md5)
                                  );

                attr_.setAttribute(  href_attr + "/" + FILE_TO_HDEP,
                                     src_dst_fdep_md5,
                                     new StringAttributeValue( src_dst_url_md5 )
                                  );
            }
        }
    }


    //-------------------------------------------------------------------------
    /**
    * Build a version of href_status map that translates all
    * values from src into md5(dst) namespace, and uses 
    * a map rather than a list of strings (this makes 
    * comparisons of the old list & new map easy later on).
    */
    //-------------------------------------------------------------------------
    HashMap<String,Pair<Integer,HashMap<String,String>>> make_dst_status_map(
        HrefStatusMap href_status_map,
        String        dst_webapp_url_base,
        String        src_webapp_url_base,
        int           src_webapp_url_base_length,
        int           src_store_length,
        String        dst_store,
        MD5           md5)
    {
        // Extract the raw map (avoids the need to use syncrhonized func)
        Map<String,Pair<Integer,List<String>>> src_status_map = 
                                               href_status_map.getStatusMap();

        HashMap<String,Pair<Integer,HashMap<String,String>>> 
            src_dst_status_md5_map = 
                new HashMap<String,Pair<Integer,HashMap<String,String>>>(
                    src_status_map.size());

        for ( String src_url  :  src_status_map.keySet() )
        {
            String dst_url;
            if ( ! src_url.startsWith( src_webapp_url_base ) )
            {
                dst_url = src_url;
            }
            else
            {
                dst_url = dst_webapp_url_base +
                          src_url.substring( src_webapp_url_base_length );
            }

            String dst_url_md5 = md5.digest( dst_url.getBytes() );

            Pair<Integer,List<String>>  src_status = 
                                        src_status_map.get( src_url );

            List<String> src_fdep_list  = src_status.getSecond();   // maybe null

            HashMap<String,String> src_dst_fdep_md5_map = 
                new HashMap<String,String>();

            if (src_fdep_list != null )
            {
                for ( String src_fdep : src_fdep_list )
                {
                    String dst_fdep = 
                       dst_store + src_fdep.substring( src_store_length );
                    String dest_fdep_md5 = md5.digest( dst_fdep.getBytes());
                    src_dst_fdep_md5_map.put( dest_fdep_md5, null );
                }
            }

            // Note:  src_dst_fdep_md5_map will never be null, 
            //        but it might be empty

            src_dst_status_md5_map.put(dst_url_md5, 
                                       new Pair<Integer,HashMap<String,String>>(
                                           src_status.getFirst(),
                                           src_dst_fdep_md5_map));
        }

        return src_dst_status_md5_map;
    }

    /**
    *  Merges href difference into destnation table (e.g.: for staging)
    */
    public void mergeHrefDiff( HrefDifference href_diff)
                throws         AVMNotFoundException,
                               SocketException,
                               SSLException,
                               LinkValidationAbortedException
    {
        MD5    md5                        = new MD5();
        String dst_store                  = href_diff.getDstStore();
        String src_store                  = href_diff.getSrcStore();
        int    src_store_length           = src_store.length();
        String dst_webapp_url_base        = href_diff.getDstWebappUrlBase();
        String src_webapp_url_base        = href_diff.getSrcWebappUrlBase();
        int    src_webapp_url_base_length = src_webapp_url_base.length();
        String href_attr                  = href_diff.getHrefAttr();


        // Build various concordances & lookup tables for this changeset:
        //
        Map<String,String> newmod_file  = new HashMap<String, String>();             
        Map<String,String> href_in_conc = new HashMap<String, String>();
        HashMap<String, HashMap<String,String>>  newmod_conc;
        HashMap<String, HashMap<String,String>>  deleted_conc;
        List<HrefManifestEntry>                  newmod_manifest_list;

        newmod_conc          = new HashMap<String, HashMap<String,String>>();
        deleted_conc         = new HashMap<String, HashMap<String,String>>();
        newmod_manifest_list = new ArrayList<HrefManifestEntry>();

        build_changeset_concordances(
             href_diff, 
             newmod_conc,             // updated by this call
             deleted_conc,            // updated by this call
             newmod_file,             // updated by this call
             href_in_conc,            // updated by this call
             newmod_manifest_list,
             dst_webapp_url_base,
             src_webapp_url_base,
             src_webapp_url_base_length,
             src_store_length,
             dst_store,
             md5);


        // Now we have:
        //
        //   newmod_conc           Contains as keys any new or modified 
        //                         dst_url_md5.  Its values are a map keys of 
        //                         dst_file_md5 (that have null values).
        //                         
        //   deleted_conc          Contains as keys any deleted dst_url_md5.
        //                         its values are a map of the dst_file_md5 
        //                         (that have null values).
        //                         
        //   newmod_file           Contains as keys any new or modified 
        //                         dst_file_md5.  Its values are the associated 
        //                         dst_file
        //                         
        //   href_in_conc          Contains as keys dst_url_md5 that is present
        //                         in either newmod_conc or deleted_conc.  Its
        //                         values are the associated dst_url.  When the
        //                         href appears in newmod_conc but not 
        //                         deleted_conc, its value in is the actual url,
        //                         not null.  This serves as a flag that allows 
        //                         some calls to set md5 -> url to be skipped.
        //
        //   newmod_manifest_list  Like the manifest_list, only all values are
        //                         the md5sum of their translation into the dst
        //                         namespace.
        //
        //
        // The old HREF_TO_SOURCE needs to be updated with the delta as follows:
        //
        //    For each URL in href_in_conc, 
        //       o  Get old HREF_TO_SOURCE (if null, create empty set).
        //       o  Remove from this set any file in deleted_conc or newmod_file
        //       o  Add back to this set any file in newmod_conc
        //       o  Update HREF_TO_SOURCE
        //       o  If href_in_conc value is non-null, update MD5_TO_HREF
        //       o  If new HREF_TO_SOURCE is empty, remove the href:
        //              - [2] HREF_TO_SOURCE
        //              - [3] HREF_TO_STATUS
        //              - [4] STATUS_TO_HREF
        //              - [6] MD5_TO_HREF
        //              - [7] HREF_TO_FDEP
        //              - [8] FILE_TO_HREF
        //
        //
        //  Update href status/dep info for eveything in the href_status_map
        //        o  Get old status via HREF_TO_STATUS
        //           If not there or different:
        //             [3]  Update status
        //             [4]  Remove prior status from STATUS_TO_HREF if necessary
        //
        //        o  Get old dependency list from HREF_TO_FDEP
        //           For every file not in href_status_map, 
        //                remove it from new HREF_TO_FDEP and FILE_TO_HDEP.
        //           For every file in href_status_map not in old HREF_TO_FDEP
        //                add it to new HREF_TO_FDEP and FILE_TO_HDEP.
        //
        //
        //    For each deleted file in:  deleted_file_md5_map
        //              
        //       o  Remove:
        //              - [1] SOURCE_TO_HREF
        //
        //       o  If HREF_TO_FDEP for the file is empty, then remove:
        //              - [5] MD5_TO_FILE
        //
        //    Also, ensure: 
        //            any new files get an md5 -> file mapping
        //            any new hrefs get an md5 -> href mapping
        //

        merge_href_to_source_and_source_to_href( 
            href_diff,
            newmod_conc,
            deleted_conc,
            newmod_file,
            href_in_conc,
            newmod_manifest_list);  


        // Ensure any new or modified file has an associated md5 -> file mapping
        for ( String file_md5 : newmod_file.keySet() )
        {
            attr_.setAttribute( 
                    href_attr + "/" + MD5_TO_FILE,
                    file_md5,
                    new StringAttributeValue( newmod_file.get( file_md5 )));
        }


        //
        // Update href status info
        //


        // Get the status of the modified links.
        HrefStatusMap  href_status_map  = href_diff.getHrefStatusMap();

        HashMap<String,Pair<Integer,HashMap<String,String>>>
            src_dst_status_md5_map  =  make_dst_status_map( 
                                            href_status_map,
                                            dst_webapp_url_base,
                                            src_webapp_url_base,
                                            src_webapp_url_base_length,
                                            src_store_length,
                                            dst_store,
                                            md5);


        // Reset the status of the urls, if necessary.
        // 
        // Because src_dst_status_md5_map is derived from src_status_map 
        // by translating values into the dst namespace and taking 
        // their md5sum, all operations below can work directly
        // using md5(dst) values.


        update_href_status( href_attr, src_dst_status_md5_map);


        //
        // Clean up obsolete file info when a file has been deleted
        //

        Map<String,String>  deleted_file_md5_map;
        deleted_file_md5_map = href_diff.getDeletedFileMd5();

        for (String deleted_file_md5 : deleted_file_md5_map.keySet() )
        {
            attr_.removeAttribute(href_attr + "/" + SOURCE_TO_HREF,
                                  deleted_file_md5);

            Attribute old_hdep_attribute = 
                attr_.getAttribute( href_attr    + "/" + 
                                    FILE_TO_HDEP + "/" + 
                                    deleted_file_md5);

            // Let's see if any hrefs depend on the deleted file
            if ( old_hdep_attribute != null )
            {
                if ( old_hdep_attribute.size() == 0 )
                {
                    attr_.removeAttribute( href_attr   + "/" + FILE_TO_HDEP,
                                           deleted_file_md5);

                    // No hrefs depend on this, so md5 -> file isn't needed
                    attr_.removeAttribute( href_attr   + "/" + MD5_TO_FILE,
                                           deleted_file_md5);
                }
            }
            else
            {
                // No hrefs depend on this, so md5 -> file isn't needed
                attr_.removeAttribute( href_attr   + "/" + MD5_TO_FILE,
                                       deleted_file_md5);
            }
        }

        recheckBrokenLinks(href_diff,
                           dst_webapp_url_base,
                           src_webapp_url_base,
                           src_webapp_url_base_length,
                           src_store_length,
                           dst_store,
                           href_attr,
                           md5);
    }


    //-------------------------------------------------------------------------
    /**
    *   Walk the set of links believed to be broken looking for hrefs that 
    *   are no longer broken (due to adding a file, fixing a server, etc.).
    */
    //-------------------------------------------------------------------------
    public void recheckBrokenLinks( HrefDifference href_diff,
                                    String         dst_webapp_url_base,
                                    String         src_webapp_url_base,
                                    int            src_webapp_url_base_length,
                                    int            src_store_length,
                                    String         dst_store,
                                    String         href_attr,
                                    MD5            md5 )
                throws              AVMNotFoundException,
                                    SocketException,
                                    SSLException,
                                    LinkValidationAbortedException
    {
        List<Pair<String, Attribute>> links = 
            attr_.query( href_attr + "/" + STATUS_TO_HREF, 
                         new AttrAndQuery(new AttrQueryGTE( "" + 400 ),
                                          new AttrQueryLTE( "" + 599 )));

        if  ( links == null ) {return;}

        HrefStatusMap  href_status_map = new HrefStatusMap();

        int    connect_timeout     = href_diff.getConnectTimeout();
        int    read_timeout        = href_diff.getReadTimeout();

        for ( Pair<String, Attribute> link : links  )
        {
            String  response_code_str = link.getFirst();
            int     response_code     = Integer.parseInt( response_code_str );
            Set<String> href_md5_set  = link.getSecond().keySet();

            for ( String href_md5 : href_md5_set )
            {
                String href_str = 
                   attr_.getAttribute( href_attr   + "/" + 
                                       MD5_TO_HREF + "/" + 
                                       href_md5
                                     ).getStringValue();

                boolean get_lookup_dependencies = 
                            href_str.startsWith( dst_webapp_url_base);

                validate_href( 
                    href_str,                 // href to revalidate
                    href_status_map,          // new status map 
                    get_lookup_dependencies,  // only when url is internal
                    false,                    // don't fetch urls in result
                    connect_timeout,          // same as original href_diff
                    read_timeout,             // same as original href_diff
                    null);                    // don't monitor progress
            }
        }


        // TODO: double check this... 

        HashMap<String,Pair<Integer,HashMap<String,String>>>
            src_dst_status_md5_map  =  make_dst_status_map( 
                                            href_status_map,
                                            dst_webapp_url_base,
                                            src_webapp_url_base,
                                            src_webapp_url_base_length,
                                            src_store_length,
                                            dst_store,
                                            md5);
        
        update_href_status( href_attr, src_dst_status_md5_map);


        return; 
    }


    /*-------------------------------------------------------------------------
    *  extract_links_from_dir --
    *------------------------------------------------------------------------*/
    void  extract_links_from_dir( int                    version,
                                  String                 dir,
                                  String                 fqdn,
                                  int                    port,
                                  String                 req_path,
                                  HrefManifest           href_manifest,
                                  HrefStatusMap          href_status_map,
                                  int                    connectTimeout,
                                  int                    readTimeout,
                                  HrefValidationProgress progress,
                                  int                    depth) 
          throws                  AVMNotFoundException,
                                  SocketException, 
                                  SSLException,
                                  LinkValidationAbortedException
    {
        Map<String, AVMNodeDescriptor> entries = null;

        // Ensure that the virt server is virtualizing this version

        try
        {
            // e.g.:   42, "mysite:/www/avm_webapps/ROOT/moo"
            entries = avm_.getDirectoryListing( version, dir );
        }
        catch (Exception e)     // TODO: just AVMNotFoundException ?
        {
            if ( log.isErrorEnabled() )
            {
                log.error("Could not list version: " + version + 
                         " of directory: " + dir + "  " + e.getMessage());
            }
            return;
        }

        for ( Map.Entry<String, AVMNodeDescriptor> entry  : entries.entrySet() )
        {
            String            entry_name = entry.getKey();
            AVMNodeDescriptor avm_node   = entry.getValue();
            String            avm_path   = dir +  "/" + entry_name;

            if ( (depth == 0) &&
                 (entry_name.equalsIgnoreCase("META-INF")  || 
                  entry_name.equalsIgnoreCase("WEB-INF")
                 )
               )
            {
                continue;
            }

            if  ( avm_node.isDirectory() )
            {
                extract_links_from_dir( version,
                                        avm_path,
                                        fqdn,
                                        port,
                                        req_path + "/" + entry_name,
                                        href_manifest,
                                        href_status_map,
                                        connectTimeout,
                                        readTimeout,
                                        progress,
                                        depth + 1);

                // stats for monitoring
                if ( progress != null )
                {
                    progress.incrementDirUpdateCount();  
                }
            }
            else
            {
                extract_links_from_file( avm_path,
                                         fqdn,
                                         port,
                                         req_path + "/" + entry_name,
                                         href_manifest,
                                         href_status_map,
                                         connectTimeout,
                                         readTimeout,
                                         progress);

                // stats for monitoring
                if ( progress != null )
                {
                    progress.incrementFileUpdateCount();  
                }
            }
        }
    }

        
    /*-------------------------------------------------------------------------
    *  extract_links_from_file --
    *------------------------------------------------------------------------*/
    void  extract_links_from_file( String                 src_path,
                                   String                 fqdn,
                                   int                    port,
                                   String                 req_path,
                                   HrefManifest           href_manifest,
                                   HrefStatusMap          href_status_map,
                                   int                    connectTimeout,
                                   int                    readTimeout,
                                   HrefValidationProgress progress) 
          throws                   AVMNotFoundException,
                                   SocketException, 
                                   SSLException,
                                   LinkValidationAbortedException
    {
        String implicit_url = null;
        try 
        {
            // You might think that URLEncoder or URI would be
            // able to encode individual segements, but URLEncoder
            // turns " " into "+" rather than "%20".   That's ok for
            // x-www-form-encoding, but not for the request path;
            // the URL class just lets " " remain " ".  Nothing in
            // class contains a cross reference to URI and this issue.

            URI u = new URI( "http",       // scheme
                             null,         // userinfo
                             fqdn,         // host
                             port,         // port
                             req_path,     // request path
                             null,         // query
                             null);        // frag

            implicit_url = u.toASCIIString();
        }
        catch (Exception e)
        { 
            if ( log.isErrorEnabled() ) { log.error(e.getMessage()); }
            return;
        }


        List<String> urls = validate_href( implicit_url,
                                           href_status_map,
                                           true,            // get lookup dep
                                           true,            // get urls
                                           connectTimeout,
                                           readTimeout,
                                           progress);


        boolean saw_gen_url = false;

        ArrayList<String> href_arraylist;
        if ( urls == null ) 
        { 
            href_arraylist = new ArrayList<String>( 1 ); 
        }
        else
        {
            href_arraylist = new ArrayList<String>( urls.size() + 1 );

            for (String  resp_url  : urls )
            {
                if ( ! saw_gen_url  && implicit_url.equals(resp_url))
                {
                    saw_gen_url = true;
                }
                href_arraylist.add( resp_url );
            }
        }

        // add imlicit (dead reckoned) url if not contained in file
        if ( ! saw_gen_url )  { href_arraylist.add( implicit_url ); }


        href_manifest.add( new HrefManifestEntry( src_path, href_arraylist ));
    }

    /*-------------------------------------------------------------------------
    *  validate_href --
    *        Validate one hyperlink
    *------------------------------------------------------------------------*/
    List<String>  validate_href( String                 url_str,
                                 HrefStatusMap          href_status_map,
                                 boolean                get_lookup_dependencies,
                                 boolean                get_urls,
                                 int                    connect_timeout,
                                 int                    read_timeout,
                                 HrefValidationProgress progress) 
                  throws         SocketException, 
                                 SSLException,
                                 LinkValidationAbortedException
    {
        HttpURLConnection conn = null; 
        URL               url  = null;
        int               response_code;

        // Allow operation to be aborted before the potentially 
        // long-running act of pulling on a URL.

        if ( progress != null && progress.isAborted() )
        {
            throw new LinkValidationAbortedException();
        }

        try 
        { 
            url = new URL( url_str );

            // Oddly, url.openConnection() does not actually
            // open a connection; it merely creates a connection
            // object that is later opened via connect() within
            // the HrefExtractor.
            //
            conn = (HttpURLConnection) url.openConnection(); 
        }
        catch (Exception e )
        {
            // You could have a bogus protocol or some other probem.
            // For example:           <a href="sales@alfresco.com">woops</a>
            // Gives you              url_str="sales@alfresco.com"
            // and exception msg: no protocol: sales@alfresco.com

            if ( log.isErrorEnabled() )
            {
                log.error("Cannot connect to :" + url_str );
            }

            // Rather than update the URL status just let it retain 
            // whatever status it had before, and assume this is
            // an ephemeral network failure;  "ephemeral" here means 
            // "on all instances of this url for this validation."
            //
            // TODO -- rethink this.

            return null;
        }

        if ( get_lookup_dependencies )
        {
            conn.addRequestProperty(
                CacheControlFilter.LOOKUP_DEPENDENCY_HEADER, "true" );
        }

        // "Infinite" timeouts that aren't really infinite
        // are a bad idea in this context.  If it takes more 
        // than 15 seconds to connect or more than 60 seconds 
        // to read a response, give up.  
        //
        HrefExtractor href_extractor = new HrefExtractor();

        conn.setConnectTimeout( connect_timeout );  // e.g.:  10000 milliseconds
        conn.setReadTimeout(    read_timeout    );  // e.g.:  30000 milliseconds
        conn.setUseCaches( false );                 // handle caching manually

        if ( log.isDebugEnabled() )
        {
            log.debug("About to fetch: " + url_str );
        }

        href_extractor.setConnection( conn );

        try { response_code = conn.getResponseCode(); }
        catch ( SocketException se )
        {
            // This could be either of two major problems:
            //   java.net.SocketException
            //      java.net.ConnectException        likely: Server down
            //      java.net.NoRouteToHostException  likely: firewall/router

            if  ( get_lookup_dependencies )   // If we're trying to get lookup
            {                                 // dependencies, this is a fatal
                throw se;                     // error fetching virtualized
            }                                 // content, so rethrow.
            else                              
            {                                 // It's an external link, so
                response_code = 400;          // just call it a link failure.
            }
        }
        catch ( SSLException ssle )
        {
            // SSL issues
            if  ( get_lookup_dependencies )   // If we're trying to get lookup
            {                                 // dependencies, this is a fatal
                throw ssle;                   // error fetching virtualized
            }                                 // content, so rethrow.
            else
            {                                 // It's an external link, so
                response_code = 400;          // just call it a link failure.
            }
        }
        catch (IOException ioe)
        {
            // java.net.UnknownHostException 
            // .. or other things, possibly due to a mist-typed url
            // or other bad/interrupted request.
            //
            // Even if this is a local link, let's keep going.

            if ( log.isDebugEnabled() )
            {
                log.debug("Could not fetch response code: " + ioe.getMessage());
            }
            response_code = 400;                // probably a bad request
        }


        if ( log.isDebugEnabled() )
        {
            log.debug("Response code for '" + url_str + "': " + response_code);
        }
        
        // deal with resonse code 

        if ( ! get_lookup_dependencies  ||  
             ( response_code < 200 || response_code >= 300)
           )
        { 
            // The remainder of this function deals with tracking lookup 
            // dependencies.  Because  we only care about the links that
            // URL's page contains if we're tracking dependencies, do
            // an early return here.

            href_status_map.put( 
                url_str, new Pair<Integer,List<String>>(response_code,null) );

            if  ( progress != null )
            {
                progress.incrementUrlUpdateCount();
            }

            return null; 
        } 

        // Rather than just fetch the 1st LOOKUP_DEPENDENCY_HEADER 
        // in the response, to be paranoid deal with the possiblity that 
        // the information about what AVM files have been accessed is stored 
        // in more than one of these headers (though it *should* be all in 1).
        //
        // Unfortunately, getHeaderFieldKey makes the name of the 0-th header 
        // return null, "even though the 0-th header has a value".  Thus the 
        // loop below is 1-based, not 0-based. 
        //
        // "It's a madhouse! A madhouse!" 
        //            -- Charton Heston playing the character "George Taylor"
        //               Planet of the Apes, 1968
        //

        ArrayList<String> dependencies = new ArrayList<String>();

        String header_key = null;
        for (int i=1; (header_key = conn.getHeaderFieldKey(i)) != null; i++)
        {
            if (!header_key.equals(CacheControlFilter.LOOKUP_DEPENDENCY_HEADER))
            { 
                continue; 
            }

            String header_value = null;
            try 
            { 
                header_value = 
                    URLDecoder.decode( conn.getHeaderField(i), "UTF-8"); 
            }
            catch (Exception e)
            {
                if ( log.isErrorEnabled() )
                {
                    log.error("Skipping undecodable response header: " + 
                              conn.getHeaderField(i));
                }
                continue;
            }

            // Each lookup dependency header consists of a comma-separated
            // list file names.
            String [] lookup_dependencies = header_value.split(", *");

            for (String dep : lookup_dependencies )
            {
                dependencies.add( dep );
            }
        }

        // files upon which url_str URL depends.
        href_status_map.put( 
           url_str, new Pair<Integer,List<String>>(response_code,dependencies));

        if ( progress != null )
        {
            progress.incrementUrlUpdateCount();
        }

        if ( ! get_urls ) 
        { 
            return null; 
        }

        List<String> extracted_hrefs = null;
        try
        {
            extracted_hrefs = href_extractor.extractHrefs();
        }
        catch (Exception e) 
        { 
            if ( log.isErrorEnabled() ) 
            { 
                log.error("Could not parse: " + url_str ); 
            }
        }

        return extracted_hrefs;
    }


    //------------------------------------------------------------------------
    //------------------------------------------------------------------------
    //------------------------------------------------------------------------
    //------------------------------------------------------------------------
    //------------------------------------------------------------------------
    //------------------------------------------------------------------------
    //------------------------------------------------------------------------
    //------------------------------------------------------------------------
    //------------------------------------------------------------------------
    //------------------------------------------------------------------------
    //------------------------------------------------------------------------
    //------------------------------------------------------------------------


    /*-------------------------------------------------------------------------
    *  getHrefManifestEntry --
    *        
    *------------------------------------------------------------------------*/
    public HrefManifestEntry getHrefManifestEntry( String path,
                                         int    statusGTE,
                                         int    statusLTE) 
                             throws      AVMNotFoundException 
    {
        MD5 md5         = new MD5();
        String path_md5 =  md5.digest( path.getBytes() );

        ValidationPathParser p = 
            new ValidationPathParser(avm_, path);

        String store           = p.getStore();
        String webapp_name     = p.getWebappName();
        String app_base        = p.getAppBase();
        String dns_name        = p.getDnsName();

        if (webapp_name == null ) 
        { 
            throw new RuntimeException("Not a path to a file: " + path);
        }

        String status_gte = "" + statusGTE;
        String status_lte = "" + statusLTE;

        // Example value:  ".href/mysite"
        String store_attr_base =  
               getAttributeStemForDnsName( dns_name, false, true);

        int version = avm_.getLatestSnapshotID( store );
        //--------------------------------------------------------------------
        // NEON:       faking latest snapshot version and just using HEAD
        version = -1;  // NEON:  TODO remove this line & replace with a JMX
                       //        call to load the version if it isn't already.
                       //
                       //        Question:  how long should the version stay
                       //                   loaded if a load was required?
        //--------------------------------------------------------------------


        // Example value: .href/mysite/|ROOT/-2
        String href_attr =  store_attr_base    + 
                            "/|" + webapp_name +
                            "/"  + LATEST_VERSION_ALIAS;


        Attribute href_attr_map = attr_.getAttribute( href_attr      + "/" +
                                                      SOURCE_TO_HREF + "/" +
                                                      path_md5
                                                    );


        ArrayList<String> href_arraylist = new ArrayList<String>();
        for ( String href_md5 : href_attr_map.keySet() )
        {
            // Filter by examining the response code,
            // but only if we're being selective.

            if ( (statusGTE > 100) || (statusLTE < 599) )
            {
                int response_code =   
                            attr_.getAttribute( href_attr      + "/" + 
                                                HREF_TO_STATUS + "/" + 
                                                href_md5
                                              ).getIntValue();
                
                if ((response_code < statusGTE) || (response_code > statusLTE))
                {
                    continue;
                }
            }

            String href_str = 
                   attr_.getAttribute( href_attr   + "/" + 
                                       MD5_TO_HREF + "/" + 
                                       href_md5
                                     ).getStringValue();

            href_arraylist.add( href_str );
        }

        return new HrefManifestEntry( path, href_arraylist);
    }

    /*-------------------------------------------------------------------------
    *  getBrokenHrefManifestEntries --
    *        
    *------------------------------------------------------------------------*/
    public List<HrefManifestEntry> getBrokenHrefManifestEntries( 
                                          String   storeNameOrWebappPath)  
                                   throws AVMNotFoundException 
    {
        return getHrefManifestEntries( storeNameOrWebappPath, 400, 599);
    }

    /*-------------------------------------------------------------------------
    *  getHrefManifestEntries --
    *        
    *------------------------------------------------------------------------*/
    public List<HrefManifestEntry> getHrefManifestEntries( 
                                      String storeNameOrWebappPath,
                                      int    statusGTE,
                                      int    statusLTE) 
                                   throws AVMNotFoundException 
    {
        ValidationPathParser p = 
            new ValidationPathParser(avm_, storeNameOrWebappPath);

        String store           = p.getStore();
        String webapp_name     = p.getWebappName();
        String app_base        = p.getAppBase();
        String dns_name        = p.getDnsName();

        String status_gte = "" + statusGTE;
        String status_lte = "" + statusLTE;

        HashMap<String, ArrayList<String> > href_manifest_map = 
            new HashMap<String, ArrayList<String> >();

        // Example value:  ".href/mysite"
        String store_attr_base =  
               getAttributeStemForDnsName( dns_name, false, true);


        int version = avm_.getLatestSnapshotID( store );
        //--------------------------------------------------------------------
        // NEON:       faking latest snapshot version and just using HEAD
        version = -1;  // NEON:  TODO remove this line & replace with a JMX
                       //        call to load the version if it isn't already.
                       //
                       //        Question:  how long should the version stay
                       //                   loaded if a load was required?
        //--------------------------------------------------------------------

        if  ( webapp_name != null )
        {
            getHrefManifestEntriesFromWebapp( href_manifest_map,
                                        webapp_name,
                                        store_attr_base,
                                        status_gte,
                                        status_lte);
        }
        else
        {
            Map<String, AVMNodeDescriptor> webapp_entries = null;

            // e.g.:   42, "mysite:/www/avm_webapps"
            webapp_entries = avm_.getDirectoryListing(version, app_base );

            for ( Map.Entry<String, AVMNodeDescriptor> webapp_entry  :
                          webapp_entries.entrySet()
                        )
            {
                webapp_name = webapp_entry.getKey();  // my_webapp
                AVMNodeDescriptor avm_node    = webapp_entry.getValue();

                if ( webapp_name.equalsIgnoreCase("META-INF")  ||
                     webapp_name.equalsIgnoreCase("WEB-INF")
                   )
                {
                    continue;
                }

                if  ( avm_node.isDirectory() )
                {
                    getHrefManifestEntriesFromWebapp( href_manifest_map,
                                                webapp_name,
                                                store_attr_base,
                                                status_gte,
                                                status_lte );
                }
            }
        }

        // The user will always want to see the list of files sorted
        ArrayList<String> file_names = 
            new ArrayList<String>( href_manifest_map.keySet() );

        Collections.sort (file_names );

        // Create sorted list from sorted keys of map
        // TODO: Would using a TreeMap be faster than this?

        ArrayList<HrefManifestEntry> href_manifest_list = 
            new ArrayList<HrefManifestEntry>(file_names.size());
        
        for (String file_name : file_names )
        {
            List<String> hlist = href_manifest_map.get( file_name );
            Collections.sort( hlist );
            href_manifest_list.add( new HrefManifestEntry(file_name, hlist ) );
        }

        return href_manifest_list; 
    }


    /*-------------------------------------------------------------------------
    *  getHrefManifestEntriesFromWebapp --
    *        
    *------------------------------------------------------------------------*/
    void getHrefManifestEntriesFromWebapp( 
            HashMap<String, ArrayList<String> > href_manifest_map, 
            String webapp_name,
            String store_attr_base,
            String status_gte,
            String status_lte)
    {
        // Example value: .href/mysite/|ROOT/-2
        String href_attr =  store_attr_base    + 
                            "/|" + webapp_name +
                            "/"  + LATEST_VERSION_ALIAS;

         
        List<Pair<String, Attribute>> links = 
            attr_.query( href_attr + "/" + STATUS_TO_HREF, 
                         new AttrAndQuery(new AttrQueryGTE(status_gte),
                                          new AttrQueryLTE(status_lte)));

        if  ( links == null ) {return;}

        HashMap<String,String> md5_to_file_cache = new HashMap<String,String>();

        for ( Pair<String, Attribute> link : links  )
        {
            Set<String> href_md5_set  = link.getSecond().keySet();

            for ( String href_md5 : href_md5_set )
            {
                String href_str = 
                   attr_.getAttribute( href_attr   + "/" + 
                                       MD5_TO_HREF + "/" + 
                                       href_md5
                                     ).getStringValue();

                Set<String> file_md5_set = 
                        attr_.getAttribute( 
                                            href_attr      + "/" + 
                                            HREF_TO_SOURCE + "/" + 
                                            href_md5
                                          ).keySet();
                

                for ( String file_md5 : file_md5_set )
                {
                    ArrayList<String> href_list;
                    String file_name = md5_to_file_cache.get( file_md5 );

                    if ( file_name != null )
                    {
                        href_list = href_manifest_map.get( file_name );
                    }
                    else
                    {
                        file_name = attr_.getAttribute( href_attr   + "/" +
                                                        MD5_TO_FILE + "/" +
                                                        file_md5
                                                      ).getStringValue();

                        md5_to_file_cache.put( file_md5, file_name );

                        href_list = new ArrayList<String>();
                        href_manifest_map.put( file_name, href_list );
                    }

                    href_list.add( href_str );
                }
            }
        }
    }



    /*-------------------------------------------------------------------------
    *  getHrefsDependentUponFile --
    *        
    *------------------------------------------------------------------------*/
    public List<String> getHrefsDependentUponFile(String path)
                        throws                    AVMNotFoundException
    {
        MD5    md5      = new MD5();
        String file_md5 =  md5.digest(path.getBytes());

        ValidationPathParser p = 
            new ValidationPathParser(avm_, path);

        String store           = p.getStore();
        String webapp_name     = p.getWebappName();
        String app_base        = p.getAppBase();
        String dns_name        = p.getDnsName();

        // Example value:  ".href/mysite"
        String store_attr_base =  getAttributeStemForDnsName( dns_name, 
                                                              false, 
                                                              true
                                                            );

        // Example value: .href/mysite/|ROOT/-2
        String href_attr =  store_attr_base    + 
                            "/|" + webapp_name +
                            "/"  + LATEST_VERSION_ALIAS;

        Set<String> dependent_hrefs_md5 = 
                         attr_.getAttribute( href_attr    + "/" + 
                                             FILE_TO_HDEP + "/" + 
                                             file_md5
                                           ).keySet();

        List<String> dependent_hrefs = 
                        new ArrayList<String>( dependent_hrefs_md5.size() );

        for (String href_md5 : dependent_hrefs_md5 )
        {
            String href_str = 
                   attr_.getAttribute( href_attr   + "/" + 
                                       MD5_TO_HREF + "/" + 
                                       href_md5
                                     ).getStringValue();

            dependent_hrefs.add( href_str );
        }

        Collections.sort( dependent_hrefs );

        return  dependent_hrefs;
    }
      


    /*-------------------------------------------------------------------------
    *  getBrokenHrefConcordanceEntries --
    *
    *------------------------------------------------------------------------*/
    public List<HrefConcordanceEntry> getBrokenHrefConcordanceEntries(
                                         String storeNameOrWebappPath ) 
                                      throws AVMNotFoundException 
    {
        return getHrefConcordanceEntries(storeNameOrWebappPath, 400, 599);
    }
    
    public List<HrefConcordanceEntry> getHrefConcordanceEntries(
                                         String storeNameOrWebappPath, 
                                         int    statusGTE,
                                         int    statusLTE) 
                                      throws AVMNotFoundException 
    {
        ValidationPathParser p = 
            new ValidationPathParser(avm_, storeNameOrWebappPath);

        String store           = p.getStore();
        String webapp_name     = p.getWebappName();
        String app_base        = p.getAppBase();
        String dns_name        = p.getDnsName();

        String status_gte = "" + statusGTE;
        String status_lte = "" + statusLTE;


        // Example value:  ".href/mysite"
        String store_attr_base =  getAttributeStemForDnsName( dns_name, 
                                                              false, 
                                                              true
                                                            );

        int version = avm_.getLatestSnapshotID( store );
        //--------------------------------------------------------------------
        // NEON:       faking latest snapshot version and just using HEAD
        version = -1;  // NEON:  TODO remove this line & replace with a JMX
                       //        call to load the version if it isn't already.
                       //
                       //        Question:  how long should the version stay
                       //                   loaded if a load was required?
        //--------------------------------------------------------------------

        List<HrefConcordanceEntry>  concordance_entries = 
            new ArrayList<HrefConcordanceEntry>();


        if  ( webapp_name != null )
        {
            getHrefsByStatusCodeFromWebapp( concordance_entries,
                                            webapp_name,
                                            store_attr_base,
                                            status_gte,
                                            status_lte);
        }
        else
        {
            Map<String, AVMNodeDescriptor> webapp_entries = null;

            // e.g.:   42, "mysite:/www/avm_webapps"
            webapp_entries = avm_.getDirectoryListing(version, app_base );

            for ( Map.Entry<String, AVMNodeDescriptor> webapp_entry  :
                          webapp_entries.entrySet()
                        )
            {
                webapp_name = webapp_entry.getKey();  // my_webapp
                AVMNodeDescriptor avm_node    = webapp_entry.getValue();

                if ( webapp_name.equalsIgnoreCase("META-INF")  ||
                     webapp_name.equalsIgnoreCase("WEB-INF")
                   )
                {
                    continue;
                }

                if  ( avm_node.isDirectory() )
                {
                    getHrefsByStatusCodeFromWebapp( concordance_entries,
                                                    webapp_name,
                                                    store_attr_base,
                                                    status_gte,
                                                    status_lte );
                }
            }
        }

        for ( HrefConcordanceEntry conc_entry : concordance_entries )
        {
            Arrays.sort ( conc_entry.getLocations() );
        }

        Collections.sort( concordance_entries );

        return concordance_entries;
    }


    /*-------------------------------------------------------------------------
    *  getHrefsByStatusCodeFromWebapp --
    *        
    *------------------------------------------------------------------------*/
    void  getHrefsByStatusCodeFromWebapp( 
                List<HrefConcordanceEntry> concordance_entries,
                String webapp_name,
                String store_attr_base,
                String status_gte,
                String status_lte)
    {
        // Example value: .href/mysite/|ROOT/-2
        String href_attr =  store_attr_base    + 
                            "/|" + webapp_name +
                            "/"  + LATEST_VERSION_ALIAS;

         
        List<Pair<String, Attribute>> links = 
            attr_.query( href_attr + "/" + STATUS_TO_HREF, 
                         new AttrAndQuery(new AttrQueryGTE(status_gte),
                                          new AttrQueryLTE(status_lte)));

        if  ( links == null ) {return;}

        for ( Pair<String, Attribute> link : links  )
        {
            String  response_code_str = link.getFirst();
            int     response_code     = Integer.parseInt( response_code_str );

            Set<String> href_md5_set  = link.getSecond().keySet();

            for ( String href_md5 : href_md5_set )
            {
                String href_str = 
                   attr_.getAttribute( href_attr   + "/" + 
                                       MD5_TO_HREF + "/" + 
                                       href_md5
                                     ).getStringValue();

                Set<String> file_md5_set = 
                        attr_.getAttribute( 
                                            href_attr      + "/" + 
                                            HREF_TO_SOURCE + "/" + 
                                            href_md5
                                          ).keySet();
                

                String [] locations = new String[  file_md5_set.size() ];
                int i=0; 
                for ( String file_md5 : file_md5_set )
                {
                    locations[i] = 
                       attr_.getAttribute( href_attr   + "/" + 
                                           MD5_TO_FILE + "/" + 
                                           file_md5
                                         ).getStringValue();
                    i++;
                }

                HrefConcordanceEntry concordance_entry = 
                  new HrefConcordanceEntry( href_str, locations, response_code);

                concordance_entries.add( concordance_entry );
            }
        }
    }


    

    /*------------------------------------------------------------------------*/
    /**
    *  Updates collection of all files in a dir that are "gone" now,
    *  and the collection of hyperlinks that depended upon them.
    *  When the total set of files that are "gone" has been
    *  established, this info is used to find broken hyperlinks
    *  that still exist within files that are not "gone".
    */
    /*------------------------------------------------------------------------*/
    void update_dir_gone_broken_hdep_cache( 
                int                    dst_version,
                String                 dst_path, 
                Map<String,String>     deleted_file_md5,
                Map<String,String>     broken_hdep_cache,
                String                 href_attr,
                MD5                    md5,
                HrefValidationProgress progress)
    {
        if ( excluder_ != null && excluder_.matches( dst_path )) 
        {
            return;
        }

        Map<String, AVMNodeDescriptor> entries = null;

        try
        {
            entries = avm_.getDirectoryListing( dst_version, dst_path);
        }
        catch (Exception e)     // TODO: just AVMNotFoundException ?
        {
            if ( log.isErrorEnabled() )
            {
                log.error("Could not list version: " + dst_version + 
                         " of directory: " + dst_path + "  " + e.getMessage());
            }
            return;
        }


        for ( Map.Entry<String, AVMNodeDescriptor> entry  : entries.entrySet() )
        {
            String            entry_name = entry.getKey();
            AVMNodeDescriptor avm_node   = entry.getValue();
            String            avm_path   = dst_path +  "/" + entry_name;

            if  ( avm_node.isDirectory() )
            {
                update_dir_gone_broken_hdep_cache( dst_version,
                                                   avm_path,
                                                   deleted_file_md5,
                                                   broken_hdep_cache,
                                                   href_attr,
                                                   md5,
                                                   progress
                                                 );
                // stats for monitoring
                if ( progress != null )
                {
                    progress.incrementDirUpdateCount();
                }
            }
            else if ( avm_node.isFile() )
            {
                update_file_gone_broken_hdep_cache( avm_path,
                                                    deleted_file_md5,
                                                    broken_hdep_cache,
                                                    href_attr,
                                                    md5,
                                                    progress
                                                  );
                // stats for monitoring
                if ( progress != null )
                {
                    progress.incrementFileUpdateCount();
                }

            }
        }
    }


    /*------------------------------------------------------------------------*/
    /**
    *  Updates collection of all files that are "gone" now,
    *  and the collection of hyperlinks that depended upon them.
    *  When the total set of files that are "gone" has been
    *  established, this info is used to find broken hyperlinks
    *  that still exist within files that are not "gone".
    */
    /*------------------------------------------------------------------------*/
    void update_file_gone_broken_hdep_cache( 
                String                 dst_path, 
                Map<String,String>     deleted_file_md5,
                Map<String,String>     broken_hdep_cache,
                String                 href_attr,
                MD5                    md5,
                HrefValidationProgress progress)
    {
        if ( excluder_ != null && excluder_.matches( dst_path )) 
        {
            return;
        }

        String file_gone_md5 =  md5.digest( dst_path.getBytes() );

        Set<String> dependent_hrefs_md5 = 
                         attr_.getAttribute( href_attr    + "/" + 
                                             FILE_TO_HDEP + "/" + 
                                             file_gone_md5
                                           ).keySet();


        for ( String href_md5 :  dependent_hrefs_md5 )
        {
             broken_hdep_cache.put(href_md5, null); 
        }
        deleted_file_md5.put( file_gone_md5, null );
    }
                                          

    /*------------------------------------------------------------------------*/
    /**
    *   Updates all href info under the specified path.
    *   The 'path' parameter should be the path to either:
    *
    *   <ul>
    *      <li> A store name (e.g.:  mysite)
    *      <li> The path to the dir containing all webapps in a store
    *           (e.g.:  mysite:/www/avm_webapps/ROOT)
    *      <li> The path to a particular webapp, or within a webapp
    *           (e.g.:  mysite:/www/avm_webapps/ROOT/moo)
    *   </ul>
    *
    *   If you give a path to a particular webapp (or within one), 
    *   only that webapp's href info is updated.  If you give a 
    *   store name, or the dir containing all webapps ina store 
    *   (e.g.: mysite:/www/avm_webapps), then the href info for 
    *   all webapps in the store are updated.
    */
    /*------------------------------------------------------------------------*/
    public void updateHrefInfo( String  storeNameOrWebappPath, 
                                boolean incremental,
                                int     connect_timeout,
                                int     read_timeout,
                                int     nthreads,    // NEON - currently ignored
                                HrefValidationProgress progress) 
                throws          AVMNotFoundException, 
                                SocketException, 
                                SSLException,
                                LinkValidationAbortedException
    {
        if ( progress != null )
        {
            progress.init();
        }

        try 
        {
            updateHrefInfo_( storeNameOrWebappPath, 
                             incremental,
                             connect_timeout,
                             read_timeout,
                             nthreads,
                             progress
                           );
        }
        finally 
        { 
            if ( progress != null )
            {
                progress.setDone( true ); 
            }
        }
    }

    /*-------------------------------------------------------------------------
    *  updateHrefInfo_ --
    *        
    *------------------------------------------------------------------------*/
    void updateHrefInfo_( String  storeNameOrWebappPath, 
                          boolean incremental,
                          int     connect_timeout,
                          int     read_timeout,
                          int     nthreads,         // NEON - currently ignored
                          HrefValidationProgress progress) 
         throws           AVMNotFoundException, 
                          SocketException, 
                          SSLException,
                          LinkValidationAbortedException
    {
        ValidationPathParser p = 
            new ValidationPathParser(avm_, storeNameOrWebappPath);

        String store           = p.getStore();
        String webapp_name     = p.getWebappName();
        String app_base        = p.getAppBase();
        String dns_name        = p.getDnsName();

        // Example value:  ".href/mysite"
        String store_attr_base =  getAttributeStemForDnsName( dns_name, 
                                                              true, 
                                                              incremental 
                                                            );

        int version = avm_.getLatestSnapshotID( store );
        //--------------------------------------------------------------------
        // NEON:       faking latest snapshot version and just using HEAD
        version = -1;  // NEON:  TODO remove this line & replace with a JMX
                       //        call to load the version if it isn't already.
                       //
                       //        Question:  how long should the version stay
                       //                   loaded if a load was required?
        //--------------------------------------------------------------------



        // In the future, it should be possible for different webapp 
        // urls should resolve to different servers.
        // http://<dns>.www--sandbox.version-v<vers>.<virt-domain>:<port>

        String virt_domain    = virtreg_.getVirtServerFQDN();
        int    virt_port      = virtreg_.getVirtServerHttpPort();
        String store_url_base = "http://" + 
                                dns_name  + 
                                ".www--sandbox.version--v" + version  + "." + 
                                virt_domain + ":" + virt_port;

        MD5 md5 = new MD5();
        if  ( webapp_name != null )
        {
            String webapp_url_base = null;
            try 
            {
                webapp_url_base = 
                    store_url_base + 
                    ( webapp_name.equals("ROOT") 
                      ? "" 
                      : ("/" + URLEncoder.encode( webapp_name, "UTF-8"))
                    );
            }
            catch (Exception e) { /* UTF-8 is supported */ }

            revalidateWebapp( store, 
                              version,
                              true,           // is_latest_version
                              store_attr_base,
                              webapp_name,
                              app_base + "/" + webapp_name,
                              webapp_url_base,
                              md5,
                              connect_timeout,
                              read_timeout,
                              progress
                            );

            // stats for monitoring
            if ( progress != null )
            {
                progress.incrementWebappUpdateCount(); 
            }
        }
        else
        {
            Map<String, AVMNodeDescriptor> webapp_entries = null;

            // e.g.:   42, "mysite:/www/avm_webapps"
            webapp_entries = avm_.getDirectoryListing(version, app_base );


            for ( Map.Entry<String, AVMNodeDescriptor> webapp_entry  :
                          webapp_entries.entrySet()
                        )
            {
                webapp_name = webapp_entry.getKey();  // my_webapp
                AVMNodeDescriptor avm_node    = webapp_entry.getValue();

                if ( webapp_name.equalsIgnoreCase("META-INF")  ||
                     webapp_name.equalsIgnoreCase("WEB-INF")
                   )
                {
                    continue;
                }

                // In the future, it should be possible for different webapp 
                // urls should resolve to different servers.
                // http://<dns>.www--sandbox.version-v<vers>.<virtdomain>:<port>

                String webapp_url_base = null;
                try 
                {
                    webapp_url_base = 
                           store_url_base + (webapp_name.equals("ROOT") ? "" : 
                           URLEncoder.encode( webapp_name, "UTF-8"));
                }
                catch (Exception e) { /* UTF-8 is supported */ }

                if  ( avm_node.isDirectory() )
                {
                    revalidateWebapp( store, 
                                      version,
                                      true,           // is_latest_version
                                      store_attr_base,
                                      webapp_name,
                                      app_base + "/" + webapp_name,
                                      webapp_url_base,
                                      md5,
                                      connect_timeout,
                                      read_timeout,
                                      progress
                                    );

                    // stats for monitoring
                    if ( progress != null )
                    {
                        progress.incrementWebappUpdateCount();  
                    }
                }
            }
        }
    }

    /*-------------------------------------------------------------------------
    *  revalidateWebapp --
    *        
    *------------------------------------------------------------------------*/
    void revalidateWebapp( String  store, 
                           int     version,
                           boolean is_latest_version,
                           String  store_attr_base,
                           String  webapp_name,
                           String  webapp_avm_base,
                           String  webapp_url_base,
                           MD5     md5,
                           int     connect_timeout,
                           int     read_timeout,
                           HrefValidationProgress progress) 
         throws            SocketException, SSLException
    {
        HashMap<String,String>  gen_url_cache   = new HashMap<String,String>();
        HashMap<String,String>  parsed_url_cache= new HashMap<String,String>();
        HashMap<Integer,String> status_cache    = new HashMap<Integer,String>();
        HashMap<String,String>  file_hdep_cache = new HashMap<String,String>();


        // The following convention is used:  
        //           version <==> (version - max - 2) %(max+2)
        //
        // The only case that ever matters for now is that:
        // -2 is an alias for the last snapshot
        //
        // Thus href attribute info for the  "last snapshot" of 
        // a store with the dns name:  preview.alice.mysite is
        // stored within attribute service under the keys: 
        //
        //      .href/mysite/alice/preview/|mywebapp/-2
        //
        // This allows entire projects and/or webapps to removed in 1 step

        // Example:               ".href/mysite/|ROOT"
        String webapp_attr_base =  store_attr_base  +  "/|"  +  webapp_name;

        if ( ! attr_.exists( webapp_attr_base ) ) 
        {
            attr_.setAttribute(store_attr_base, "|" + webapp_name, 
                               new MapAttributeValue());
        }
        
        String href_attr;
        if ( ! is_latest_version ) // add key: .href/mysite/|mywebapp/latest/42
        {
            // Because we're not validating the last snapshot,
            // don't clobber "last snapshot" info.  Instead
            // just make the href_attr info live under the
            // version specified.  

            //Example:  .href/mysite/|mywebapp/99
            attr_.setAttribute( webapp_attr_base , version, 
                                new MapAttributeValue() );

            //  href data uses the raw version key
            href_attr = webapp_attr_base +  "/" + version;
        }
        else
        {
            // Validating the latest snapshot.  Therefore, record the actual 
            // LATEST_SNAPSHOT version info, but store data under the
            // LATEST_VERSION_ALIAS key ("-2") rather than the version number.
            // This make it possible to do incremental updates more easily
            // because we're not constantly shuffling data around from 
            // exlicit version key to explicit version key.

            //Example:  .href/mysite/|mywebapp/latest -> version

            attr_.setAttribute( webapp_attr_base , 
                                LATEST_VERSION, 
                                new IntAttributeValue( version )
                              );
        
            //Example:  .href/mysite/|mywebapp/-2

            attr_.setAttribute( webapp_attr_base ,  LATEST_VERSION_ALIAS, 
                                new MapAttributeValue() );

            //  href data goes under the "-2" key:    .href/mysite/|myproject/-2
            href_attr = webapp_attr_base +  "/" + LATEST_VERSION_ALIAS;
        }


        attr_.setAttribute( href_attr, SOURCE_TO_HREF, new MapAttributeValue());
        attr_.setAttribute( href_attr, HREF_TO_SOURCE, new MapAttributeValue());
        attr_.setAttribute( href_attr, HREF_TO_STATUS, new MapAttributeValue());
        attr_.setAttribute( href_attr, STATUS_TO_HREF, new MapAttributeValue());
        attr_.setAttribute( href_attr, MD5_TO_FILE,    new MapAttributeValue());
        attr_.setAttribute( href_attr, MD5_TO_HREF,    new MapAttributeValue());
        attr_.setAttribute( href_attr, HREF_TO_FDEP,   new MapAttributeValue());
        attr_.setAttribute( href_attr, FILE_TO_HDEP,   new MapAttributeValue());

        // Info for latest snapshot (42) of mywebapp within mysite is now:
        //
        //      .href/mysite/|mywebapp/latest -> 42
        //      .href/mysite/|mywebapp/-2/source_to_href/   
        //      .href/mysite/|mywebapp/-2/href_to_source/  
        //      .href/mysite/|mywebapp/-2/href_to_status/
        //      .href/mysite/|mywebapp/-2/status_to_href/
        //      .href/mysite/|mywebapp/-2/md5_to_file/
        //      .href/mysite/|mywebapp/-2/md5_to_href/
        //      .href/mysite/|mywebapp/-2/href_to_fdep/
        //      .href/mysite/|mywebapp/-2/file_to_hdep/
        //
        // Info for latest snapshot (42) of mywebapp within mysite/alice is now:
        //
        //      .href/mysite/alice/|mywebapp/latest -> 42
        //      .href/mysite/alice/|mywebapp/-2/source_to_href/   
        //      .href/mysite/alice/|mywebapp/-2/href_to_source/  
        //      .href/mysite/alice/|mywebapp/-2/href_to_status/
        //      .href/mysite/alice/|mywebapp/-2/status_to_href/
        //      .href/mysite/alice/|mywebapp/-2/md5_to_file/
        //      .href/mysite/alice/|mywebapp/-2/md5_to_href/
        //      .href/mysite/alice/|mywebapp/-2/href_to_fdep/
        //      .href/mysite/alice/|mywebapp/-2/file_to_hdep/
        //
        // This makes it easy to delete an entire project or webapp.


        // Find dead reconning URLs for every asset in the system:
        validate_dir( version,
                      webapp_avm_base,
                      webapp_url_base, 
                      href_attr, 
                      md5,
                      gen_url_cache,
                      parsed_url_cache,
                      status_cache,
                      file_hdep_cache,
                      connect_timeout,
                      read_timeout,
                      progress,
                      0 
                    );
 
        // stats for monitoring
        if ( progress != null )
        {
            progress.incrementDirUpdateCount();  
        }


        // Now all the generated URLs have had their status checked,
        // but the parsed URLs from these files might not be yet.
        // Pull on every URL that isn't currently in the gen_cache.

        for ( Map.Entry<String,String> entry  :  parsed_url_cache.entrySet() )
        {
            String  parsed_url_md5 = entry.getKey();

            if ( gen_url_cache.containsKey( parsed_url_md5 ) )  // skip if this
            {                                                   // url validated
                continue;                                       // within the dir
            }                                                   // walking phase
            
            String  parsed_url = entry.getValue();

            validate_url( parsed_url,
                          parsed_url_md5, 
                          href_attr,
                          md5,
                          parsed_url.startsWith( webapp_url_base ),
                          status_cache,
                          file_hdep_cache,
                          connect_timeout,
                          read_timeout
                        );

             // stats for monitoring
             if ( progress != null )
             {
                 progress.incrementUrlUpdateCount();
             }
        }

        if ( progress != null )
        {
            progress.incrementWebappUpdateCount();  // for monitoring progress
        }
    }


    /*-------------------------------------------------------------------------
    *  validate_dir --
    *------------------------------------------------------------------------*/
    void validate_dir( int    version, 
                       String dir,
                       String url_base,
                       String href_attr, 
                       MD5    md5,
                       Map<String,String>  gen_url_cache,
                       Map<String,String>  parsed_url_cache,
                       Map<Integer,String> status_cache,
                       Map<String,String>  file_hdep_cache,
                       int    connect_timeout,
                       int    read_timeout,
                       HrefValidationProgress progress,
                       int    depth ) 
         throws        SocketException, SSLException
    {
        Map<String, AVMNodeDescriptor> entries = null;

        // Ensure that the virt server is virtualizing this version

        try
        {
            // e.g.:   42, "mysite:/www/avm_webapps/ROOT/moo"
            entries = avm_.getDirectoryListing( version, dir );
        }
        catch (Exception e)     // TODO: just AVMNotFoundException ?
        {
            if ( log.isErrorEnabled() )
            {
                log.error("Could not list version: " + version + 
                         " of directory: " + dir + "  " + e.getMessage());
            }
            return;
        }

        for ( Map.Entry<String, AVMNodeDescriptor> entry  : entries.entrySet() )
        {
            String            entry_name = entry.getKey();
            AVMNodeDescriptor avm_node   = entry.getValue();
            String            avm_path   = dir +  "/" + entry_name;

            if ( (depth == 0) &&
                 (entry_name.equalsIgnoreCase("META-INF")  || 
                  entry_name.equalsIgnoreCase("WEB-INF")
                 )
               )
            {
                continue;
            }

            // Don't index bogus files
            if ( excluder_ != null && excluder_.matches( avm_path )) 
            {
                continue;
            }

            String url_encoded_entry_name = null;
            try 
            {
                url_encoded_entry_name = URLEncoder.encode(entry_name, "UTF-8");
            }
            catch (Exception e) { /* UTF-8 is supported */ }


            String implicit_url = url_base  +  "/" + url_encoded_entry_name;

            if  ( avm_node.isDirectory() )
            {
                validate_dir( version, 
                              avm_path,
                              implicit_url,
                              href_attr,
                              md5,
                              gen_url_cache,
                              parsed_url_cache,
                              status_cache,
                              file_hdep_cache,
                              connect_timeout,
                              read_timeout,
                              progress,
                              depth + 1 ) ;

                // stats for monitoring
                if ( progress != null )
                {
                    progress.incrementDirUpdateCount();  
                }
            }
            else
            {
                validate_file( 
                   avm_path,
                   implicit_url,
                   md5.digest(implicit_url.getBytes()),
                   href_attr,
                   md5,
                   gen_url_cache,
                   parsed_url_cache,
                   status_cache,
                   file_hdep_cache,
                   connect_timeout,
                   read_timeout,
                   progress
                );

                // stats for monitoring
                if ( progress != null )
                {
                    progress.incrementFileUpdateCount();  
                }
            }
        }
    }

    /*-------------------------------------------------------------------------
    *  validate_file --
    *        
    *------------------------------------------------------------------------*/
    void validate_file( String                 avm_path,
                        String                 gen_url_str, 
                        String                 gen_url_md5,
                        String                 href_attr,
                        MD5                    md5,
                        Map<String,String>     gen_url_cache,
                        Map<String,String>     parsed_url_cache,
                        Map<Integer,String>    status_cache,
                        Map<String,String>     file_hdep_cache,
                        int                    connect_timeout,
                        int                    read_timeout,
                        HrefValidationProgress progress)
         throws         SocketException, SSLException
    {
        String file_md5= md5.digest(avm_path.getBytes());

        gen_url_cache.put(gen_url_md5,null);

        // A map from the href to the source files that contain it created
        // made as soon a new generated or parsed url is discovered.
        // Because of how directories are traversed when generating urls,
        // in this function we can be certain that the href being processed
        // has never been generated before, but it may have been *parsed*
        // from an earlier file.
        //
        // Therefore, a call to see if the HREF_TO_SOURCE key for this URL
        // exists can be avoided if parsed_url_cache already contains it.
        // Otherwise, we've got to do the actual 'exists' test.

        if ( ! parsed_url_cache.containsKey( gen_url_md5 )   &&
             ! attr_.exists( href_attr      + "/" + 
                             HREF_TO_SOURCE + "/" + gen_url_md5)
           )
        {
            attr_.setAttribute( href_attr + "/" + HREF_TO_SOURCE,
                                gen_url_md5,
                                new MapAttributeValue()
                              );
        }


        // Claim the url to self "appears" in this source file

        attr_.setAttribute(href_attr + "/" + HREF_TO_SOURCE + "/" + gen_url_md5,
                           file_md5,
                           new BooleanAttributeValue( true )
                          );


        attr_.setAttribute( href_attr + "/" + MD5_TO_FILE, 
                            file_md5, 
                            new StringAttributeValue( avm_path )
                          );


        List<String> urls = validate_url( gen_url_str, 
                                          gen_url_md5, 
                                          href_attr, 
                                          md5,
                                          true,     // get lookup dependencies
                                          status_cache,
                                          file_hdep_cache,
                                          connect_timeout,
                                          read_timeout);

        // stats for monitoring
        if ( progress != null )
        {
            progress.incrementUrlUpdateCount();
        }

        if ( urls == null ) 
        {
            return;
        }

        // Collect list of hrefs contained by this source file
        // If the generated URL is not already contained in the 
        // parsed URL list, add it.

        MapAttribute  href_map_attrib_value  = new MapAttributeValue();
        boolean       saw_gen_url            = false;

        for (String  response_url  : urls )
        {
            String response_url_md5 = md5.digest(response_url.getBytes());

            if ( ! saw_gen_url && response_url_md5.equals( gen_url_md5) )
            {
                saw_gen_url = true;
            }
            
            href_map_attrib_value.put( response_url_md5,
                                       new BooleanAttributeValue(true ));


            if ( ! parsed_url_cache.containsKey( response_url_md5 ) )
            {
                parsed_url_cache.put(response_url_md5, response_url);

                if ( ! gen_url_cache.containsKey( response_url_md5 ) &&
                     ! attr_.exists( href_attr      + "/" + 
                                     HREF_TO_SOURCE + "/" + 
                                     response_url_md5 )
                   )
                {
                    attr_.setAttribute( href_attr + "/" + HREF_TO_SOURCE,
                                        response_url_md5,
                                        new MapAttributeValue()
                                      );
                }
            }

            attr_.setAttribute( 
                href_attr + "/" + HREF_TO_SOURCE  + "/" + response_url_md5 ,
                file_md5,
                new BooleanAttributeValue( true ));
        }

        if ( ! saw_gen_url )
        {
            href_map_attrib_value.put( gen_url_md5, 
                                       new BooleanAttributeValue( true ));
        }

        attr_.setAttribute( href_attr + "/" + SOURCE_TO_HREF,
                            file_md5, 
                            href_map_attrib_value
                          );
    }


    /*-------------------------------------------------------------------------
    *  validate_url --
    *------------------------------------------------------------------------*/
    List<String>  validate_url( String  url_str,
                                String  url_md5,
                                String  href_attr,
                                MD5     md5,
                                boolean get_lookup_dependencies,
                                Map<Integer,String> status_cache,
                                Map<String,String>  file_hdep_cache,
                                int  connect_timeout,
                                int  read_timeout) 
                  throws        SocketException, SSLException
    {
        HttpURLConnection conn = null; 
        URL               url  = null;
        int               response_code;

        try 
        { 
            url = new URL( url_str );

            // Oddly, url.openConnection() does not actually
            // open a connection; it merely creates a connection
            // object that is later opened via connect() within
            // the HrefExtractor.
            //
            conn = (HttpURLConnection) url.openConnection(); 
        }
        catch (Exception e )
        {
            // You could have a bogus protocol or some other probem.
            // For example:           <a href="sales@alfresco.com">woops</a>
            // Gives you              url_str="sales@alfresco.com"
            // and exception msg: no protocol: sales@alfresco.com

            if ( log.isErrorEnabled() )
            {
                log.error("openConnection() cannot connect to :" + url_str );
            }

            // Rather than update the URL status just let it retain 
            // whatever status it had before, and assume this is
            // an ephemeral network failure;  "ephemeral" here means 
            // "on all instances of this url for this validation."
            //
            // TODO -- rethink this.

            return null;
        }

        if ( get_lookup_dependencies )
        {
            conn.addRequestProperty(
                CacheControlFilter.LOOKUP_DEPENDENCY_HEADER, "true" );
        }

        // "Infinite" timeouts that aren't really infinite
        // are a bad idea in this context.  If it takes more 
        // than 15 seconds to connect or more than 60 seconds 
        // to read a response, give up.  
        //
        HrefExtractor href_extractor = new HrefExtractor();

        conn.setConnectTimeout( connect_timeout );  // e.g.:  10000 milliseconds
        conn.setReadTimeout(    read_timeout    );  // e.g.:  30000 milliseconds
        conn.setUseCaches( false );                 // handle caching manually


        if ( log.isDebugEnabled() )
        {
            log.debug("About to fetch: " + url_str );
        }

        href_extractor.setConnection( conn );

        try { response_code = conn.getResponseCode(); }
        catch ( SocketException se )
        {
            // This could be either of two major problems:
            //   java.net.SocketException
            //      java.net.ConnectException        likely: Server down
            //      java.net.NoRouteToHostException  likely: firewall/router

            if  ( get_lookup_dependencies )   // If we're trying to get lookup
            {                                 // dependencies, this is a fatal
                throw se;                     // error fetching virtualized
            }                                 // content, so rethrow.
            else                              
            {                                 // It's an external link, so
                response_code = 400;          // just call it a link failure.
            }
        }
        catch ( SSLException ssle )
        {
            // SSL issues
            if  ( get_lookup_dependencies )   // If we're trying to get lookup
            {                                 // dependencies, this is a fatal
                throw ssle;                   // error fetching virtualized
            }                                 // content, so rethrow.
            else
            {                                 // It's an external link, so
                response_code = 400;          // just call it a link failure.
            }
        }
        catch (IOException ioe)
        {
            // java.net.UnknownHostException 
            // .. or other things, possibly due to a mist-typed url
            // or other bad/interrupted request.
            //
            // Even if this is a local link, let's keep going.

            if ( log.isDebugEnabled() )
            {
                log.debug("Could not fetch response code: " + ioe.getMessage());
            }
            response_code = 400;                // probably a bad request
        }


        attr_.setAttribute( href_attr + "/" + MD5_TO_HREF, 
                            url_md5, 
                            new StringAttributeValue( url_str )
                          );

        attr_.setAttribute( href_attr + "/" + HREF_TO_STATUS, 
                            url_md5, 
                            new IntAttributeValue( response_code )
                          );

        // Only initialize the response map if absoultely necessary
        // Use the status_cache to eliminate calls to test existence

        if ( ! status_cache.containsKey( response_code ) )  // maybe necessary
        {
            if ( ! attr_.exists( href_attr      + "/" +     // do actual remote
                                 STATUS_TO_HREF + "/" +     // call to see if
                                 response_code )            // this status key
               )                                            // must be created
            {                                                
                attr_.setAttribute( href_attr + "/" + STATUS_TO_HREF, 
                                    "" + response_code,
                                    new MapAttributeValue()
                                  );

            }
            status_cache.put( response_code, null );        // never check again
        }

        attr_.setAttribute( 
                href_attr + "/" + STATUS_TO_HREF + "/" + response_code,
                url_md5,
                new BooleanAttributeValue( true ));


        if ( ! get_lookup_dependencies  ||  
             ( response_code < 200 || response_code >= 300)
           )
        { 
            // The remainder of this function deals with tracking lookup 
            // dependencies.  Because  we only care about the links that
            // URL's page contains if we're tracking dependencies, do
            // an early return here.

            return null; 
        } 


        // Rather than just fetch the 1st LOOKUP_DEPENDENCY_HEADER 
        // in the response, to be paranoid deal with the possiblity that 
        // the information about what AVM files have been accessed is stored 
        // in more than one of these headers (though it *should* be all in 1).
        //
        // Unfortunately, getHeaderFieldKey makes the name of the 0-th header 
        // return null, "even though the 0-th header has a value".  Thus the 
        // loop below is 1-based, not 0-based. 
        //
        // "It's a madhouse! A madhouse!" 
        //            -- Charton Heston playing the character "George Taylor"
        //               Planet of the Apes, 1968
        //

        ArrayList<String> dependencies = new ArrayList<String>();

        String header_key = null;
        for (int i=1; (header_key = conn.getHeaderFieldKey(i)) != null; i++)
        {
            if (!header_key.equals(CacheControlFilter.LOOKUP_DEPENDENCY_HEADER))
            { 
                continue; 
            }

            String header_value = null;
            try 
            { 
                header_value = 
                    URLDecoder.decode( conn.getHeaderField(i), "UTF-8"); 
            }
            catch (Exception e)
            {
                if ( log.isErrorEnabled() )
                {
                    log.error("Skipping undecodable response header: " + 
                              conn.getHeaderField(i));
                }
                continue;
            }

            // Each lookup dependency header consists of a comma-separated
            // list file names.
            String [] lookup_dependencies = header_value.split(", *");

            for (String dep : lookup_dependencies )
            {
                dependencies.add( dep );
            }
        }

        // Now "dependencies" contains a list of all 
        // files upon which url_str URL depends.

        MapAttribute fdep_map_attrib_value = new MapAttributeValue();

        for (String file_dependency : dependencies )
        {
            String fdep_md5 = md5.digest(file_dependency.getBytes());

            if ( ! file_hdep_cache.containsKey( fdep_md5 ) )
            {
                attr_.setAttribute( href_attr + "/" + FILE_TO_HDEP,
                                    fdep_md5,
                                    new MapAttributeValue()
                                  );

                file_hdep_cache.put( fdep_md5, null );
            }

            attr_.setAttribute( href_attr + "/" + FILE_TO_HDEP + "/" + fdep_md5,
                                url_md5,
                                new BooleanAttributeValue( true )
                              );

            fdep_map_attrib_value.put( fdep_md5, 
                                       new BooleanAttributeValue( true ));
        }


        attr_.setAttribute( href_attr + "/" + HREF_TO_FDEP,
                            url_md5, 
                            fdep_map_attrib_value
                          );


        List<String> extracted_hrefs = null;
        try
        {
            extracted_hrefs = href_extractor.extractHrefs();
        }
        catch (Exception e) 
        { 
            if ( log.isErrorEnabled() ) 
            { 
                log.error("Could not parse: " + url_str ); 
            }
        }
        return extracted_hrefs;
    }



    //-----------------------------------------------------------------------
    /**
    *  If 'create' is false, fetches the key bath associated 
    *  with the dns name.
    *  <p>
    *
    *  If 'create' is true, creates keys corresponding to 
    *  the store dns_name being validated, and returns the 
    *  final key path.   If the leaf key already exists 
    *  and 'incremental' is true, then the pre-existing 
    *  leaf key will be reused;  otherwise,  this function 
    *  creates a new leaf (potentially clobbering the 
    *  pre-existing one).
    */
    //-----------------------------------------------------------------------
    String getAttributeStemForDnsName( String  dns_name,
                                       boolean create,
                                       boolean incremental )
    {
        // Given a store name X has a dns name   a.b.c
        // The attribute key pattern used is:   .href/c/b/a
        // 
        // This guarantees if a segment contains a ".", it's not a part 
        // of the store's fqdn.  Thus, "." can be used to delimit the end 
        // of the store, and the begnining of the version-specific info.
        // 

        // Construct path & create coresponding attrib entries
        StringBuilder str  = new StringBuilder( dns_name.length() );
        str.append( HREF );

        // Create top level .href key if necessary
        if ( create && ! attr_.exists( HREF ) )
        {
            MapAttribute map = new MapAttributeValue();
            attr_.setAttribute("", HREF, map );
        }

        String [] seg = dns_name.split("\\.");
        String pth;
        for (int i= seg.length -1 ; i>=0; i--) 
        { 
            if (create)
            {
                pth = str.toString();
                if ( ((i==0) && incremental == false ) ||
                     ! attr_.exists( pth + "/" + seg[i] )
                   )
                {
                    MapAttribute map = new MapAttributeValue();
                    attr_.setAttribute( pth , seg[i], map );
                }
            }

            str.append("/" + seg[i] ); 
        }
        String result = str.toString();
        if ( result == null )
        {
            throw new IllegalArgumentException("Invalid DNS name: " + dns_name);
        }
        return result;
    }
}

/*-----------------------------------------------------------------------------
*  ValidationPathParser --
*        
*----------------------------------------------------------------------------*/
class ValidationPathParser
{
    static String App_Dir_ = "/" + JNDIConstants.DIR_DEFAULT_WWW   +
                             "/" + JNDIConstants.DIR_DEFAULT_APPBASE;

    String    store_        = null;
    String    app_base_     = null;
    String    webapp_name_  = null;
    String    dns_name_     = null;
    String    path_         = null;
    String    req_path_     = null;
    int       store_end_    = -2;
    int       webapp_start_ = -2;
    int       webapp_end_   = -2;
    AVMRemote avm_          = null;

    ValidationPathParser(AVMRemote avm, String path) 
    throws               IllegalArgumentException
    {
        avm_ = avm;
        path_ = path;
    }

    String getStore()      
    { 
        if ( store_ != null ) { return store_ ; }
        store_end_ = path_.indexOf(':');

        if ( store_end_ < 0) 
        { 
            store_ = path_; 
            return store_;
        }

        if ( ! path_.startsWith( App_Dir_, store_end_ + 1 )  )
        {
            throw new IllegalArgumentException("Invalid webapp path: " + path_);
        }
        else
        {
            store_ = path_.substring(0,store_end_);
        }

        return store_;
    }

    String getAppBase()    
    { 
        if (app_base_ != null) { return app_base_; }
        if (store_ == null )   { getStore(); }
        app_base_ = store_ + ":" + App_Dir_;
        return app_base_;
    }

    String getWebappName() 
    {
        if ( webapp_name_ != null) { return webapp_name_; }
        if ( store_end_ < -1)    { getStore(); }
        if ( store_end_ < 0 )    { return null; }

        webapp_start_ = 
                path_.indexOf('/', store_end_ + 1 + App_Dir_.length()); 

        if (webapp_start_ >= 0)
        {
            webapp_end_  = path_.indexOf('/', webapp_start_ + 1);
            webapp_name_ = path_.substring( webapp_start_ +1, 
                                            (webapp_end_ < 1)
                                            ?  path_.length()
                                            :  webapp_end_ );

            if  ((webapp_name_ != null) && 
                 (webapp_name_.length() == 0)
                ) 
            { 
                webapp_name_ = null; 
            }
        }
        return webapp_name_;
    }

    /*-------------------------------------------------------------------------
    *  getRequestPath --
    *       Given an path of the form mysite:/www/avm_webapps/ROOT/m oo/bar.txt
    *       returns the non-encoded request path  ( "/mo o/bar.txt")
    *
    *       Given an path of the form mysite:/www/avm_webapps/cow/m oo/bar.txt
    *       returns the non-encoded request path  ( "/cow/mo o/bar.txt")
    *
    *------------------------------------------------------------------------*/
    String getRequestPath()
    {
        if (req_path_ != null ) { return req_path_;}

        String webapp_name = getWebappName();
        if (webapp_name == null ) { return null; }

        int req_start = -1;

        if ( webapp_name.equals("ROOT") )
        {
            req_start = webapp_start_ + "ROOT".length() + 1;
        }
        else
        {
            req_start = webapp_start_;
        }

        req_path_ =  path_.substring( req_start, path_.length() );

        if ( req_path_.equals("") ) { req_path_ = "/"; }
                
        return req_path_;
    }


    String getDnsName() throws AVMNotFoundException
    { 
        if ( dns_name_ != null ) { return dns_name_; }
        if ( store_ == null )    { getStore() ; }

        dns_name_ = lookupStoreDNS( avm_, store_ );
        if ( dns_name_ == null ) 
        { 
            throw new AVMNotFoundException(
                       "No DNS entry for AVM store: " + store_);
        }
        return dns_name_;
    }

    String lookupStoreDNS(AVMRemote avm,  String store )
    {
        Map<QName, PropertyValue> props = 
                avm.queryStorePropertyKey(store, 
                     QName.createQName(null, SandboxConstants.PROP_DNS + '%'));

        return ( props.size() != 1 
                 ? null
                 : props.keySet().iterator().next().getLocalName().
                         substring(SandboxConstants.PROP_DNS.length())
               );
    }
}
