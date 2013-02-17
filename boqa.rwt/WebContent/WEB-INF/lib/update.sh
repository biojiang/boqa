#!/bin/bash
#
# Basic bash script to update the contents of this drawer
# with a content of a plugins drawer. Also creates a
# file called update-classpath.sh to update the classpath.
#

UPDATE_CP_SCRIPT=update-classpath.sh
echo "#!/bin/bash" >$UPDATE_CP_SCRIPT
chmod a+x $UPDATE_CP_SCRIPT

for OLDNAME in *.jar
do
	BASENAME=`echo $OLDNAME | sed -e s/\_.*//`
	NEWFULLNAME=`find plugins -name "$BASENAME\_*"`
	
	NEWNAME=`basename $NEWFULLNAME`
	
	if [ ! "$NEWNAME" = "$OLDNAME" ]; then
		svn mv $OLDNAME $NEWNAME
		cp $NEWFULLNAME $NEWNAME
		
		echo "sed .classpath -e s/$OLDNAME/$NEWNAME/ >temp && mv temp .classpath" >>$UPDATE_CP_SCRIPT
	fi;
	
done
