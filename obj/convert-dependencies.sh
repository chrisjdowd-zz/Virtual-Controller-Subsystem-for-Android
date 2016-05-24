#!/bin/sh
# AUTO-GENERATED FILE, DO NOT EDIT!
if [ -f $1.org ]; then
  sed -e 's!^C:/adt-bundle-windows/cygwin/lib!/usr/lib!ig;s! C:/adt-bundle-windows/cygwin/lib! /usr/lib!ig;s!^C:/adt-bundle-windows/cygwin/bin!/usr/bin!ig;s! C:/adt-bundle-windows/cygwin/bin! /usr/bin!ig;s!^C:/adt-bundle-windows/cygwin/!/!ig;s! C:/adt-bundle-windows/cygwin/! /!ig;s!^D:!/cygdrive/d!ig;s! D:! /cygdrive/d!ig;s!^C:!/cygdrive/c!ig;s! C:! /cygdrive/c!ig;' $1.org > $1 && rm -f $1.org
fi
