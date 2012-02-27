#!/bin/bash -x
#
# Basic bash script to update the contents of this drawer
# with a content of a plugins drawer
#

for OLDNAME in *.jar
do
	BASENAME=`echo $OLDNAME | sed -e s/\_.*//`
	NEWFULLNAME=`find plugins -name "$BASENAME\_*"`
	NEWNAME=`basename $NEWFULLNAME`
	svn mv $OLDNAME $NEWNAME
	cp $NEWFULLNAME $NEWNAME 
done
