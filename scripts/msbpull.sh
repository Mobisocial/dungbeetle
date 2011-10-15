#!/bin/bash

echo "Pulling Musubi backup..."
if [ $# -eq 0 ]
then
  adb pull /sdcard/MusubiBackup/MUSUBI.db || exit
else
  adb -s $1 pull /sdcard/MusubiBackup/MUSUBI.db || exit
fi
rlwrap sqlite3 MUSUBI.db
