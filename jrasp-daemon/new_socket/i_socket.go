package new_socket

type IAgentSocket interface {
	SendExit()

	Handler(p *Package)

	SendParameters(message string)

	// 具体命令：刷新命令
	SendFlushCommand(isForceFlush string)

	// 广播消息
	SendGroup(message string, t byte)
}
