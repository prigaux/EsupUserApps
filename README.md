EsupUserApps
===========

Generate layout.json similar to uPortal's

* [Introduction](https://github.com/EsupPortail/ProlongationENT#introduction) (from ProlongationENT)
* [Configuration](#configuration)
* [Shibboleth](#shibboleth)
* [Technical details](#technical-details)

Required: LDAP, CAS (or similar : Shibboleth...)

Optional: Agimus


Configuration
-------------------

* create ```src/main/webapp/WEB-INF/config.json``` similar to ```config-example.json```
* create ```src/main/webapp/WEB-INF/config-auth.json``` similar to ```config-auth-example.json```
* create ```src/main/webapp/WEB-INF/config-apps.json``` similar to ```config-apps-example.json```
* deploy using ```mvn package``` (or modify ```build.properties``` and use ```ant deploy```)
* test using ```https://ent.univ.fr/EsupUserApps/layout``` (if you get "Unauthorized", retry after logging into CAS)


Shibboleth
-------------------

For shibbolethized applications, the simpler is to integrate CAS-isified EsupUserApps.
Your users will get EsupUserApps, whereas other users will have nothing.

You can also use shibbolethized EsupUserApps:
* configure ```config-shibboleth.json```, create a new random key for ```proxyKeys```
* in the apache of your application, do:

```apache
SSLProxyEngine on
<Location /EsupUserApps>
  ProxyPass https://ent.univ.fr/EsupUserApps
</Location>
<Location /EsupUserApps/layout> 
  AuthType shibboleth
  ShibRequireSession Off
  require shibboleth
  ShibUseHeaders On
  RequestHeader set EsupUserApps-Proxy-Key XXXrandomXXX
</Location>
```

* in the application:

```html
<script> window.prolongation_ENT_args = { current: 'xxx', layout_url: '/EsupUserApps/layout' } </script>
<script src="https://ent.univ.fr/ProlongationENT/loader.js"></script>
```



Technical details
-------------------

### all urls

#### ```/purgeCache```

The configuration files are loaded on startup (or webapp reload). If you modify a file, call ```/purgeCache``` to take changes into account (when debugging, you can use config option ```disableServerCache```)

#### ```/layout```

It tries to authenticate the user (using CAS) and then computes its "layout". Configure ```config-apps.json``` first!

It is mostly compatible with uPortal's ```/layout.json```

#### ```/detectReload```

Helper url to detect if the user wants to reload the page.

When detected, code in ```lib/main.ts``` will trigger an update of the bandeau (it will first display the sessionStorage version, but will asap try to get updated version)

#### ```/redirect```

Redirects to an application using its code (aka fname). Not really useful anymore...

#### ```/logout```

#### ```/canImpersonate```

To be used as the ```CAN_IMPERSONATE_URL```, cf [impersonate documentation](docs/impersonate.md).
