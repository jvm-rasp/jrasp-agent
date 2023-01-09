package utils

import (
	"encoding/json"
	"fmt"
)

func ToString(t interface{}) string {
	bytes, err := json.Marshal(t)
	if err != nil {
		fmt.Printf("json err:%v\n", err)
		return "unknow"
	}
	return string(bytes)
}
