/**
 *
 *  Asus RT-AC68U Router VPN Handler
 *
 *  Author: Ron Meldiner
 *  Date: 2018-09-10
 */
 metadata {
	definition (name: "Asus RT-AC68U Router VPN Handler", namespace: "meldiner", author: "Ron Meldiner") {
        capability "Switch"
	}

	simulator {
		// TODO: define status and reply messages here
	}

	preferences {
    	input "routerUrl", "text", title: "URL", description: "Your Asus Router URL", required: true
		input "username", "text", title: "Username", description: "Your Asus Router Username", required: true
		input "password", "password", title: "Password", description: "Your Asus Router Password", required: true
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
    
    activate(routerUrl, vpncPppoeUsername, vpncPppoePassword, vpncHeartbeatX, vpncProto, vpncType, vpncAutoConn);
}

def off() {
	log.debug "Executing 'off'"

	def vpncPppoeUsername = ""
    def vpncPppoePassword = ""
    def vpncHeartbeatX = ""
    def vpncProto = "disable"
    def vpncType = ""
    def vpncAutoConn = ""

    activate(routerUrl, vpncPppoeUsername, vpncPppoePassword, vpncHeartbeatX, vpncProto, vpncType, vpncAutoConn);
}


//helper methods

private getAuthorizationHeader() {
   	def userpassascii = "${username}:${password}"
	def userpass = userpassascii.encodeAsBase64().toString()
    return userpass
}

private login(routerUrl, authHeader, responseHandler) {
	log.debug "Executing 'login'"    

    def params = [
        uri: routerUrl,
        path: "/login.cgi",
        headers: [
            Connection: "keep-alive",
            Pragma: "no-cache",
            "Upgrade-Insecure-Requests": 1,
            "Content-Type": "application/x-www-form-urlencoded",
            Accept: "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8",
            Referer: routerUrl + "/Main_Login.asp",
            "Accept-Encoding": "gzip, deflat",
            "Accept-Language": "en-US,en;q=0.9,he;q=0.8"
        ],
        body: "group_id=&action_mode=&action_script=&action_wait=5&current_page=Main_Login.asp&next_page=index.asp&login_authorization=" + authHeader
    ]
    try {
        httpPost(params, responseHandler);
    } catch (e) {
        log.debug "something went wrong with login: $e"
    }
}

private setVpn(routerUrl, cookie, vpncPppoeUsername, vpncPppoePasswd, vpncHeartbeatX, vpncProto, vpncType, vpncAutoConn, responseHandler) {
	log.debug "Executing 'setVpn'"    
    
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
    body += "&" + "vpnc_pptp_options_x"
    body += "&" + "vpnc_pptp_options_x_list=<auto<auto"
    body += "&" + "vpnc_des_edit="
    body += "&" + "vpnc_svr_edit="
    body += "&" + "vpnc_account_edit="
    body += "&" + "vpnc_pwd_edit="
    body += "&" + "selPPTPOption=auto"

    def params = [
        uri: routerUrl,
        path: "/start_apply.htm",
        headers: [
            "Upgrade-Insecure-Requests": 1,
            "Content-Type": "application/x-www-form-urlencoded",
            Accept: "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8",
            Referer: routerUrl + "/Advanced_VPNClient_Content.asp",
            "Accept-Encoding": "gzip, deflat",
            "Accept-Language": "en-US,en;q=0.9,he;q=0.8",
            Cookie: cookie
        ],
        body: body
    ]
    
    try {
   		httpPost(params, responseHandler);
    } catch (e) {
        log.debug "something went wrong with setVpn: $e"
    }
}

private getCookie(response) {
    return response.headers['Set-Cookie'].value.split(';')[0]
}

private activate(routerUrl, vpncPppoeUsername, vpncPppoePassword, vpncHeartbeatX, vpncProto, vpncType, vpncAutoConn) {
    def cookie = ""
    
    def setVpnRespHandler = { resp ->
        log.debug "setVpn response: ${resp.data}"
    }
    
    def loginRespHandler = { resp ->
        cookie = getCookie(resp)
        log.debug "cookie: ${cookie}"
        setVpn(routerUrl, cookie, vpncPppoeUsername, vpncPppoePassword, vpncHeartbeatX, vpncProto, vpncType, vpncAutoConn, setVpnRespHandler);
	};
    
    login(routerUrl, getAuthorizationHeader(), loginRespHandler);
}