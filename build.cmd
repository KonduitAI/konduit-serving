mvn %1 clean install -Djavacpp.platform=windows-x86_64 -Denforcer.skip=true -Ppython,uberjar,tar -Dmaven.test.skip=true
