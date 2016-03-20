/**
 *
 *  Asus TM-AC1900 Router
 *
 *  Author: Ron Meldiner
 *  Date: 2016-03-12
 */
 metadata {
	definition (name: "Asus TM-AC1900 Router", namespace: "meldiner", author: "Ron Meldiner") {
		capability "Polling"
        capability "Refresh"
        
		attribute "vpnStatus", "string"
        attribute "vpnService", "string"
	}

	simulator {
		// TODO: define status and reply messages here
	}

	preferences {
    	input "ip", "text", title: "IP", description: "Your Asus TM-AC1900 Router IP", required: true
		input "username", "text", title: "Username", description: "Your Asus TM-AC1900 Router Username", required: true
		input "password", "password", title: "Password", description: "Your Asus TM-AC1900 Router Password", required: true
	}

	tiles(scale: 2) {
    	multiAttributeTile(name:"vpnStatus", type: "generic", width: 6, height: 4){
			tileAttribute ("device.vpnStatus", key: "PRIMARY_CONTROL") {
              attributeState "off", label: "Off", backgroundColor: "#FFFFFF"
              attributeState "on", label: "On", backgroundColor: "#79b821"
            }
            
            tileAttribute ("device.vpnService", key: "SECONDARY_CONTROL") {
                attributeState "default", label: "${currentValue}"
            }
        }
        
        standardTile("refresh", "device.status", width: 1, height: 1, inactiveLabel: false, decoration: "flat") {
        	state "refresh", action:"refresh.refresh", icon:"st.secondary.refresh"
        }
        
       	main "vpnStatus"

    	// defines what order the tiles are defined in
    	details(["vpnStatus", "refresh"])
	}
}

def updated() {
    log.debug "Executing 'updated'"
}

def refresh() {
    log.debug "Executing 'refresh'"
    
    poll()
}

def poll() {
	log.debug "Executing 'poll'"
    
    delayBetween([hubGet("/Advanced_VPNClient_Content.asp"), hubGet("/ajax_status.xml")], delayInterval())
}

// parse events into attributes
def parse(String description) {
	log.debug "Executing 'parse'"
    
	def msg = parseLanMessage(description)
    def body = msg.body.replaceAll("[^\\x20-\\x7e]", "")
    
	if (body.find("html")) {
    	parseVpnClientContentHtml(body)
    } else {
        def xml = parseXml(body)
		parseStatusXml(xml)
    }
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

private hubGet(String path) {    
    device.deviceNetworkId = getDeviceNetworkId()
    
	def headers = [:] 
    headers.put("HOST", getHostAddress())
    headers.put("Authorization", getAuthorizationHeader())
    
    def result = new physicalgraph.device.HubAction(
        method: "GET",
        path: path,
        headers: headers
    )
    
    return result
}

private parseVpnClientContentHtml(html) {
	log.debug "Executing parseVpnClientContentHtml"
    
    def hiddenFieldName = "vpnc_heartbeat_x"
    def regex = '^.*name="' + hiddenFieldName + '" value="'
    def vpnService = "Error"
    
    if (html.indexOf(hiddenFieldName) > 0) {
    	vpnService = html.replaceFirst(regex, '')
    	vpnService = vpnService.take(vpnService.indexOf('">'));
        
        if (vpnService == null || vpnService == "") {
        	vpnService = "disconnected"
        }
    }
    
    log.debug "sendEvent(name: vpnService, value: " + vpnService + ")"
    sendEvent(name: "vpnService", value: vpnService)
}

private parseStatusXml(xml) {
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
            if (value == "4") {
            	value = "off"
            } else if (value == "2") {
            	value = "on"
            }
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

private int delayInterval() {
 return 8000 // milli seconds delay between sending commands
}
