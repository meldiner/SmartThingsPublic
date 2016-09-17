/**
 *
 *  Asus TM-AC1900 Router VPN Handler
 *
 *  Author: Ron Meldiner
 *  Date: 2016-03-12
 */
 metadata {
	definition (name: "Asus TM-AC1900 Router VPN Handler", namespace: "meldiner", author: "Ron Meldiner") {
		capability "Polling"
        capability "Switch"
        
		attribute "vpnStatus", "string"
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
	}

	tiles(scale: 2) {
        
        standardTile("vpnStatus", "device.vpnStatus", width: 4, height: 4, inactiveLabel: false, decoration: "flat") {
        	state "4", label: "Off", backgroundColor: "#FFFFFF", action: "Switch.on", nextState:"..."
            state "2", label: 'On', backgroundColor: "#79b821", action:"Switch.off", nextState:"..."
            state "1", label: "Connecting", action:"", nextState:"..."
            state "...", label: "...", action:"", nextState:"..."
        }
        standardTile("poll", "device.vpnStatus", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
        	state "refresh", action:"polling.poll", icon:"st.secondary.refresh"
        }
        
       	main "vpnStatus"

    	// defines what order the tiles are defined in
    	details(["vpnStatus", "poll"])
	}
}

def updated() {
    log.debug "Executing 'updated'"
}

def poll() {
	log.debug "Executing 'poll'"
    
    hubGet("/ajax_status.xml")
}

def on() {
	log.debug "Executing 'on'"
    
    def vpncPppoeUsername = getVpncPppoeUsername()
    def vpncPppoePasswd = getVpncPppoePassword()
    def vpncHeartbeatX = getVpnServer()
    def vpncProto = "l2tp"
    def vpncType = "PPTP"
    def vpncAutoConn = "1"
    
    //delayBetween([setVpn(vpncPppoeUsername, vpncPppoePasswd, vpncHeartbeatX, vpncProto, vpncType, vpncAutoConn), poll()], delayInterval())
    setVpn(vpncPppoeUsername, vpncPppoePasswd, vpncHeartbeatX, vpncProto, vpncType, vpncAutoConn)
}

def off() {
	log.debug "Executing 'off'"

	def vpncPppoeUsername = ""
    def vpncPppoePasswd = ""
    def vpncHeartbeatX = ""
    def vpncProto = "disable"
    def vpncType = ""
    def vpncAutoConn = ""
    
    delayBetween([setVpn(vpncPppoeUsername, vpncPppoePasswd, vpncHeartbeatX, vpncProto, vpncType, vpncAutoConn), poll()], delayInterval())
}

def parse(String description) {
	log.debug "Executing 'parse'"
    
    device.deviceNetworkId = new Random().nextInt()

	def msg = parseLanMessage(description)
    def body = msg.body.replaceAll("[^\\x20-\\x7e]", "")
    
    parseResponse(body)
}

//helper methods

private getIp() {
	return settings.ip
}

private getPort() {
	return 80
}

private getHostAddress() {
    return "${getIp()}:${getPort()}"
}

private getDeviceNetworkId() {
    //Need to set the devices network ID - ip:port in hex
	def hosthex = convertIPtoHex(getIp()).toUpperCase() 
    def porthex = convertPortToHex(getPort()).toUpperCase()
	return "$hosthex:$porthex" 
}

private getUsername() {
	return settings.username
}

private getPassword() {
	return settings.password
}

private getVpncPppoeUsername() {
	return settings.vpncPppoeUsername
}

private getVpncPppoePassword() {
	return settings.vpncPppoePassword
}

private getVpnServer() {
	return settings.vpnServer
}

private getAuthorizationHeader() {
   	def userpassascii = "${getUsername()}:${getPassword()}"
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

private int delayInterval() {
 return 3000 // milli seconds delay between sending commands
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
    
    log.debug "Sending hub request: " + result
    return result
}

private parseResponse(body) {
	log.debug "Executing parseResponse: " + body

	if (body.startsWith("<")) {
    	try {
            def xml = parseXml(body)
            parseStatusXml(xml)
        } catch (e) {
            log.debug "Failed parsing response as XML: " + body
        }
    } else {
    	log.debug "Body doesn't start with '<': " + body
    }
}

private parseStatusXml(xml) {
	log.debug "Executing parseStatusXml"

	xml.children().each { 
        switch (it.name()) {
    		case "vpn":
    			parseVpnStatus(it.toString())
                break
            case "usb":
            case "qtn":
            case "wan":
    		default:
            	break
		}
    }
}

private parseVpnStatus(str) {
	def key = str.split('=')[0]
    def value = str.split('=')[1]
    
    switch (key) {
        case "vpnc_state_t":
            log.debug "sendEvent(name: vpnStatus, value:" + value + ")"
        	sendEvent(name: "vpnStatus", value: value)
        	break
        case "vpnc_sbstate_t":
        case "vpn_client1_state":
        case "vpn_client2_state":
        case "vpnd_state":
        default:
        	//TODO: handle other values
        	break
	}
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