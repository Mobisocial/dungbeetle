#!/bin/bash

echo "Pushing Musubi backup..."
if [ $# -eq 0 ]
then
  adb push MUSUBI.db /sdcard/MusubiBackup/MUSUBI.db || exit
else
  adb -s $1 push MUSUBI.db /sdcard/MusubiBackup/MUSUBI.db || exit
fi
