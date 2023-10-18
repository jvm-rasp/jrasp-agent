package utils

import "strings"

// Java进程exe特征 JAVA_EXE_SUFFIX
var JAVA_EXE_SUFFIX = []string{"java", "java.exe"}

func IsJavaExe(exe string) bool {
	for _, item := range JAVA_EXE_SUFFIX {
		if strings.HasSuffix(exe, item) {
			return true
		}
	}
	return false
}
