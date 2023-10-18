package utils

import "strings"

func SplitContent(token string, split string) ([]string, error) {
	token = strings.Replace(token, " ", "", -1) // 字符串去掉"\n"和"空格"
	token = strings.Replace(token, "\n", "", -1)
	tokenArray := strings.Split(token, split)
	return tokenArray, nil
}
