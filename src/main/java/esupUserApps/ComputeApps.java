package esupUserApps;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static esupUserApps.Ldap.getFirst;

class ComputeApps {
    Conf.Main conf;
    Groups groups;
    Ldap ldap;
    Shibboleth shibboleth;
    Logger log = LoggerFactory.getLogger(ComputeApps.class);
    
    ComputeApps(Conf.Main conf) {
        this.conf = conf;
        ldap = new Ldap(conf.ldap);
        groups = new Groups(conf.GROUPS);
    }

    Set<String> computeValidApps(String uid, boolean wantImpersonate) {
        return computeValidApps(uid, getLdapPeopleInfo(uid), wantImpersonate);
    }

    Ldap.Attrs getLdapPeopleInfo(String uid) {
        return ldap.getLdapPeopleInfo("uid", uid, compute_wanted_attributes());
    }

    Ldap.Attrs getShibbolethUserInfo(HttpServletRequest request) {
        if (shibboleth == null) shibboleth = new Shibboleth(conf.shibboleth, request.getServletContext());

        Ldap.Attrs attrs = shibboleth.getUserInfo(request, compute_wanted_attributes());
        attrs.remove("uid"); // "uid" is no good information in a federation. We do not ask for it, but sometimes IDP gives it.        
        String eppn = getFirst(attrs, "eduPersonPrincipalName");
        if (eppn != null) {
            ldap.mergeAttrs(attrs, ldap.getLdapPeopleInfo("eduPersonPrincipalName", eppn, compute_wanted_attributes()));
        }
        return attrs;
    }
    
    Set<String> computeValidApps(String user, Ldap.Attrs person, boolean wantImpersonate) {
        if (person == null) return new HashSet<>();

        Map<String, Boolean> groupsCache = new HashMap<>();

        Set<String> r = new HashSet<>();
        for (String appId : conf.APPS.keySet()) {
            App app_ = conf.APPS.get(appId);
            ACLs app = wantImpersonate ? app_.admins : app_;
            if (app == null) continue;
            Boolean found = false;

            if (user.equals("*")) {
                found = true;
            }
            if (app.users != null) {
                if (app.users.contains(user))
                    found = true;
            }

            if (!found && app.groups != null) {
                for (String group : app.groups) {
                    if (groups.hasGroup(person, group, groupsCache))
                        found = true;
                }
            }
            if (found && validDates(app_)) r.add(appId);
        }
        return r;
    }
    
    Set<String> canImpersonate(String uid, String service) {
        Set<String> appIds = computeValidApps(uid, true);
        if (service != null) {
            // cleanup url
            service = service.replace(":443/", "/");
            for (String appId : new HashSet<>(appIds)) {
                App app = conf.APPS.get(appId);
                boolean keep = app.serviceRegex != null && service.matches(app.serviceRegex);
                if (!keep) appIds.remove(appId);
            }
        }
        return appIds;
    }
  
    Set<String> canAccess(String uid) {
        return computeValidApps(uid, false);
    }

    private Set<String> compute_wanted_attributes() {
        Set<String> r = groups.needed_ldap_attributes();
        r.add("memberOf"); // hard code memberOf
        r.addAll(conf.wanted_user_attributes);
        return r;
    }

    private boolean validDates(App app) {
        return (app.startDate == null || app.startDate.before(new Date())) &&
               (app.endDate == null || app.endDate.after(new Date()));
    }
    
}
