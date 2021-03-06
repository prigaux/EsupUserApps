package esupUserApps;

import javax.servlet.*;

import org.jasig.cas.client.session.SingleSignOutFilter;
import org.jasig.cas.client.authentication.AuthenticationFilter;
import org.jasig.cas.client.validation.Cas20ProxyReceivingTicketValidationFilter;

import static esupUserApps.Utils.*;

public class WebXml implements ServletContextListener {
        
    public void contextDestroyed(ServletContextEvent event) {}

    public void contextInitialized(ServletContextEvent event) {
        configure(event.getServletContext());
    }

    private void configure(ServletContext sc) {
        Conf.Main conf = Main.getConf(sc);
                
        addFilter(sc, "CAS Single Sign Out", SingleSignOutFilter.class, null,
                  "/layout", "/login");

        addFilter(sc, "CAS Authentication", AuthenticationFilter.class,
                  asMap("casServerLoginUrl", conf.cas_login_url)
                   .add("serverName", url2host(conf.EsupUserApps_url)),
                  "/login");

        addFilter(sc, "CAS Validate", Cas20ProxyReceivingTicketValidationFilter.class,
                  asMap("casServerUrlPrefix", conf.cas_base_url)
                   .add("serverName", url2host(conf.EsupUserApps_url))
                   .add("redirectAfterValidation", "false"), 
                  "/layout", "/login");
    
        addServlet(sc, "EsupUserApps", Main.class, null, Main.mappings);
    }
}
