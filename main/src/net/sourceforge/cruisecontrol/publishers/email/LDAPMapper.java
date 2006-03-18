/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2006, ThoughtWorks, Inc.
 * 651 W Washington Ave. Suite 600
 * Chicago, IL 60661 USA
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *     + Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *
 *     + Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *
 *     + Neither the name of ThoughtWorks, Inc., CruiseControl, nor the
 *       names of its contributors may be used to endorse or promote
 *       products derived from this software without specific prior
 *       written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ********************************************************************************/
package net.sourceforge.cruisecontrol.publishers.email;

import net.sourceforge.cruisecontrol.CruiseControlException;
import org.apache.log4j.Logger;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import java.util.Hashtable;

/**
 * @author laineesa
 */
public class LDAPMapper extends EmailAddressMapper {

    private static final Logger LOG = Logger.getLogger(LDAPMapper.class);

    private String url = null;
    private String ctxFactory = "com.sun.jndi.ldap.LdapCtxFactory"; // commonly used default value
    private String rootDN = null;
    private String searchTmpl = "(cn=?)";                           // commonly used default value
    private String searchAttr = "mail";                              // commonly used default value
    private DirContext ctx = null;

    /**
     *
     */
    public LDAPMapper() {
        super();
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public void setRootDN(String rootDN) {
        this.rootDN = rootDN;
    }

    public String getRootDN() {
        return rootDN;
    }

    public void setSearchTmpl(String searchTmpl) {
        this.searchTmpl = searchTmpl;
    }

    public String getSearchTmpl() {
        return searchTmpl;
    }

    public void setSearchAttr(String searchAttr) {
        this.searchAttr = searchAttr;
    }

    public String getSearchAttr() {
        return searchAttr;
    }

    public void setCtxfactory(String ctxFactory) {
        this.ctxFactory = ctxFactory;
    }

    public String getCtxFactory() {
        return ctxFactory;
    }

    public void validate() throws CruiseControlException {
        if (getUrl() == null) {
            throw new CruiseControlException("'url' not specified in configuration file.");
        }
        if (getRootDN() == null) {
            throw new CruiseControlException("'rootDN' not specified in configuration file.");
        }
    }

    /*
     *  (non-Javadoc)
     * @see net.sourceforge.cruisecontrol.publishers.email.EmailAddressMapper#open()
     */
    public void open() throws CruiseControlException {
        Hashtable env = new Hashtable();

        env.put(Context.INITIAL_CONTEXT_FACTORY, ctxFactory);  // use jndi provider
        env.put(Context.PROVIDER_URL, url);                    // the ldap url to connect to; e.g. "ldap://ca.com:389"

        try {
            ctx = new InitialDirContext(env);
            LOG.debug("LDAPMapper: InitialContext created.");
        } catch (Exception e) {
            throw new CruiseControlException(e);
        }
    }

    public void close() {
        if (ctx != null) {
            try {
                ctx.close();
                LOG.debug("LDAPMapper: InitialContext closed.");
            } catch (Exception ignored) {
                //Ignored
            }
        }
    }

    public String mapUser(String user) {
        String[] searchAttrs = {searchAttr};
        /* specify search constraints to search subtree */
        SearchControls constraints1 = new SearchControls();

        constraints1.setSearchScope(SearchControls.SUBTREE_SCOPE);
        constraints1.setCountLimit(0);
        constraints1.setTimeLimit(0);

        constraints1.setReturningAttributes(searchAttrs);

        String email = null;
        StringBuffer s = new StringBuffer(searchTmpl);
        int idx = s.toString().indexOf("?");
        s.replace(idx, idx + 1, user);
        try {
            NamingEnumeration ne = ctx.search(rootDN, s.toString(), constraints1);
            while (ne.hasMore()) {
                Object o = ne.next();
                Attributes attrs = ((SearchResult) o).getAttributes();
                Attribute emailAttr = attrs.get(searchAttr);
                email = (String) emailAttr.get();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        LOG.debug("LDAPMapper: Mapping " + user + " to " + email);

        return email;
    }
}