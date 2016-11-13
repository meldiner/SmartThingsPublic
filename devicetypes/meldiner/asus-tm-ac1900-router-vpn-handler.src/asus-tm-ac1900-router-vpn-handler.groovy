/**
 *
 *  Asus TM-AC1900 Router VPN Handler
 *
 *  Author: Ron Meldiner
 *  Date: 2016-03-12
 */
 metadata {
	definition (name: "Asus TM-AC1900 Router VPN Handler", namespace: "meldiner", author: "Ron Meldiner") {
        capability "Switch"
	}

	simulator {
		// TODO: define status and reply messages here
	}

	preferences {
    	input "ip", "text", title: "IP", description: "Your Asus TM-AC1900 Router IP", required: true
		input "username", "text", title: "Username", description: "Your Asus TM-AC1900 Router Username", required: true
		input "password", "password", title: "Password", description: "Your Asus TM-AC1900 Router Password", required: true
        input "vpncPppoeUsername", "text", title: "VPN Client PPPOE Username", description: "Your VPN Service Provider Client PPPOE Username", required: true
        input "vpncPppoePassword", "password", title: "VPN Client PPPOE Password", description: "Your VPN Service Provider Client PPPOE Password", required: true
        input "vpnServer", "text", title: "VPN Server", description: "The VPN server to connect to", required: true
        input "multipleDevices", "boolean", title: "Multiple VPN Connections", description: "Do you yse multiple VPN connections?", required: true
        input "delay", "decimal", title: "Delay Between Commands", description: "Seconds of dealy to wait bwtween executing commands", required: true, defaultValue: 2
	}

	tiles(scale: 2) {
        standardTile("vpnOn", "null", width: 2, height: 2) {
			state "on", label: "On", action: "Switch.on", icon: "st.switches.switch.on", defaultState: true
		}
        
        standardTile("vpnOff", "null", width: 2, height: 2) {
			state "off", label: "Off", action: "Switch.off", icon: "st.switches.switch.off", defaultState: true
		}
        
    	details(["vpnOn", "vpnOff"])
	}
}

def updated() {
    log.debug "Executing 'updated'"
}

def on() {
	log.debug "Executing 'on'"
    
    def vpncPppoeUsername = vpncPppoeUsername
    def vpncPppoePassword = vpncPppoePassword
    def vpncHeartbeatX = vpnServer
    def vpncProto = "l2tp"
    def vpncType = "PPTP"
    def vpncAutoConn = "1"
    
    setVpn(vpncPppoeUsername, vpncPppoePassword, vpncHeartbeatX, vpncProto, vpncType, vpncAutoConn)
}

def off() {
	log.debug "Executing 'off'"

	def vpncPppoeUsername = ""
    def vpncPppoePassword = ""
    def vpncHeartbeatX = ""
    def vpncProto = "disable"
    def vpncType = ""
    def vpncAutoConn = ""
    
    setVpn(vpncPppoeUsername, vpncPppoePassword, vpncHeartbeatX, vpncProto, vpncType, vpncAutoConn)
}

def randomizeDeviceNetworkId() {
    log.debug "Randomizing device network id"
    device.deviceNetworkId = new Random().nextInt()
}

//helper methods

private getPort() {
	return 80
}

private getHostAddress() {
    return "${ip}:${getPort()}"
}

private getDeviceNetworkId() {
    //Need to set the devices network ID - ip:port in hex
	def hosthex = convertIPtoHex(ip).toUpperCase() 
    def porthex = convertPortToHex(getPort()).toUpperCase()
	return "$hosthex:$porthex" 
}

private getAuthorizationHeader() {
   	def userpassascii = "${username}:${password}"
	def userpass = "Basic " + userpassascii.encodeAsBase64().toString()
    return userpass
}

private String convertIPtoHex(ipAddress) { 
   String hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02x', it.toInteger() ) }.join()
   return hex

}

private String convertPortToHex(port) {
   String hexport = port.toString().format( '%04x', port.toInteger() )
   return hexport
}

private hubGet(path) {
	log.debug "Executing hubGet"
    
	def headers = [:]
    return hubRequest("GET", path, headers, "")
}

private hubPost(path, headers, body) {
	return hubRequest("POST", path, headers, body)
}

private hubRequest(method, path, headers, body) {
	log.debug "Executing hubRequest"

    device.deviceNetworkId = getDeviceNetworkId()
    
    headers.put("HOST", getHostAddress())
    headers.put("Authorization", getAuthorizationHeader())
    
    def result = new physicalgraph.device.HubAction(
        method: method,
        path: path,
        headers: headers,
        body: body
    )
    
    if (multipleDevices) {
    	runIn(delay, randomizeDeviceNetworkId)
    }
    
    log.debug "Sending hub request: " + result
    return result
}

private setVpn(vpncPppoeUsername, vpncPppoePasswd, vpncHeartbeatX, vpncProto, vpncType, vpncAutoConn) {
	log.debug "Executing 'setVpn'"
    
	def headers = [:]
    
    headers.put("Content-Type", "application/x-www-form-urlencoded")
    
    def body = "current_page=Advanced_VPNClient_Content.asp"
    body += "&" + "next_page=Advanced_VPNClient_Content.asp"
    body += "&" + "modified=0"
    body += "&" + "flag=background"
    body += "&" + "action_mode=apply"
    body += "&" + "action_script=restart_vpncall"
    body += "&" + "action_wait=3"
    body += "&" + "preferred_lang=EN"
    body += "&" + "firmver=3.0.0.4"
    body += "&" + "vpnc_pppoe_username=${vpncPppoeUsername}"
    body += "&" + "vpnc_pppoe_passwd=${vpncPppoePasswd}"
    body += "&" + "vpnc_heartbeat_x=${vpncHeartbeatX}"
    body += "&" + "vpnc_dnsenable_x=1"
    body += "&" + "vpnc_proto=${vpncProto}"
    body += "&" + "vpnc_type=${vpncType}"
    body += "&" + "vpnc_auto_conn=${vpncAutoConn}"
    body += "&" + "vpn_client_unit=1"
    body += "&" + "vpn_client1_username="
    body += "&" + "vpn_client1_password="
    body += "&" + "vpn_clientx_eas="
    body += "&" + "vpnc_appendix=1<1<1<1<1"
    body += "&" + "vpnc_des="
    body += "&" + "vpnc_svr="
    body += "&" + "vpnc_account="
    body += "&" + "vpnc_pwd="
    
    hubPost("/start_apply.htm", headers, body)
}