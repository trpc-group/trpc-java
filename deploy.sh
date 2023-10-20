#!/bin/sh

# Run the tutorial
# sh deploy.sh "version number" "is it the official version" (true/false, default false)
# Such as `sh deploy.sh 0.14.0 true` to release the official version of 0.14.0
is_formal=false
version_suffix=-SNAPSHOT
if [ $# -eq 0 ];then
  echo "Please call correct argument to run this command!"
fi
if [ $# -ge 2 ]; then
  if [ $2 = "true" ]||[ $2 = "false" ];then
    is_formal=$2
  fi
fi
version=$1
if [ $is_formal = "false" ];then
  version=$1$version_suffix
fi
reply=y
read -p "Is it going to release version $version? [y/n]: " reply
if [ "$reply"x != "y"x ];then
  exit 0
fi

# Whether to skip maven test
is_skip=true
reply=y
read -p "Is it going to skip test(the default is y)? [y/n]: " reply
if [ "$reply"x = "n"x ];then
  is_skip=false
fi

# Execute mvn install or mvn deploy
echo "What maven command is to be executed?"
echo "1. install"
echo "2. deploy"
reply=1
read -p "Please input the correct serial number(the default is 1): " reply
maven_command=install
if [ "$reply"x = "2"x ];then
  maven_command=deploy
fi

# Upgrade pom.xml version
cur_path=`pwd`
cd $cur_path/trpc-dependencies/trpc-bom
mvn versions:set -DnewVersion=$version
cd $cur_path/trpc-dependencies/trpc-dependencies-bom
mvn versions:set -DnewVersion=$version
cd $cur_path
mvn versions:set -DnewVersion=$version
# Modify the tRPC-Java version in the Version.java file
sed -i "" '34s/.*/    public static final String VERSION = "v'$1'";/' $cur_path/trpc-core/src/main/java/com/tencent/trpc/core/common/Version.java
if [ $is_formal = "true" ];then
  sed -i "" '40s/.*/    public static final boolean IS_FORMAL_VERSION = true;/' $cur_path/trpc-core/src/main/java/com/tencent/trpc/core/common/Version.java
elif [ $is_formal = "false" ];then
  sed -i "" '40s/.*/    public static final boolean IS_FORMAL_VERSION = false;/' $cur_path/trpc-core/src/main/java/com/tencent/trpc/core/common/Version.java
fi
echo "mvn clean -U javadoc:jar $maven_command -Dmaven.test.skip=$is_skip -s ~/.m2/settings.xml"
if [ $maven_command = "install" ];then
  mvn clean -U $maven_command -Dmaven.test.skip=$is_skip -s ~/.m2/settings.xml
elif [ $maven_command = "deploy" ];then
  mvn -U javadoc:jar $maven_command -Dmaven.test.skip=$is_skip -s ~/.m2/settings.xml
fi