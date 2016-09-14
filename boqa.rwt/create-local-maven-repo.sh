#!/bin/bash

mvn org.apache.maven.plugins:maven-install-plugin:2.3.1:install-file -Dfile=WebContent/WEB-INF/lib/org.eclipse.rap.ui.forms_2.0.0.20130111-1314.jar -Dsource=WebContent/WEB-INF/lib/org.eclipse.rap.ui.forms.source_2.0.0.20130111-1314.jar -DgroupId=de.sonumina.boqa -DartifactId=org.eclipse.rap.ui.forms -Dversion=2.0.0.20130111-1314 -Dpackaging=jar -DlocalRepositoryPath=local-maven-repo