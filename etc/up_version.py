#!/bin/python
import pprint
import xml.dom.minidom
from xml.dom.minidom import Node
import urllib2
import json
import sys
import subprocess
from subprocess import call
import tempfile

HTTP_METADATA = "http://mobisocial.stanford.edu/files/dungbeetle_version.json"
SCP_HOST = "aemon@prpl.stanford.edu"
MANIFEST = "AndroidManifest.xml"
REMOTE_DIR = "/var/www/mobisocial/files/"
METADATA = "dungbeetle_version.json"
APK = "dungbeetle-debug.apk"

doc = xml.dom.minidom.parse(MANIFEST)

node = doc.getElementsByTagName("manifest")[0]
localVersionCode = int(node.attributes["android:versionCode"].value)
localVersionName = node.attributes["android:versionName"].value

print "Updating to " + localVersionName + ", " + str(localVersionCode)

def upload(metadata_obj):
    f = tempfile.NamedTemporaryFile(delete=False)
    f.write(json.dumps(obj))
    f.close()
    call(["scp", f.name,  SCP_HOST + ":" + REMOTE_DIR + "/" + METADATA])
    call(["ssh", SCP_HOST, "chmod", "644", REMOTE_DIR + "/" + METADATA])
    call(["scp", "./bin/" + APK,  SCP_HOST + ":" + REMOTE_DIR])
    call(["ssh", SCP_HOST, "chmod", "644", REMOTE_DIR + "/" + APK])


if len(sys.argv) > 1 and sys.argv[1] == "bootstrap":
    obj = {"versionCode" : localVersionCode, "versionName" : localVersionName }
    upload(obj)
else:
    response = urllib2.urlopen(HTTP_METADATA)
    src = response.read()
    versionObj = json.loads(src)
    remoteVersionCode = versionObj["versionCode"]
    remoteVersionName = versionObj["versionName"]
    print "Current remote versionCode is " + str(remoteVersionCode)
    print "Uploading local versionCode " + str(localVersionCode)
    assert localVersionCode > remoteVersionCode
    obj = {"versionCode" : localVersionCode, "versionName" : localVersionName }
    upload(obj)


