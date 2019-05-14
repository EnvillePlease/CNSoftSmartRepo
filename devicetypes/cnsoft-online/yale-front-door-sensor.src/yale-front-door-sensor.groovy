/**
 *Version 0.2 - Added guide to use on settings page
 *Version 0.1 - First version arms/home/disarm the alarm
 */
preferences {
	input("userName", "text", title: "Username", description: "Your username for Yale Home System")
	input("password", "password", title: "Password", description: "Your Password for Yale Home System")
	input("zonenumber", type: "number", title: "Row Number - Put 0 in here and open a window/door, refresh and see if the status changes", description: "Sensor ID")
	input description: "Once you have filled in your details \nUse “Switch off” to Disarm in any mode \nUse “Lock” to Home Arm (Arm Stay) \nUse “Switch on” to Fully Arm (Arm away).", title: "Guide", displayDuringSetup: false, type: "paragraph", element: "paragraph"
}

metadata {
definition (name: "Yale Front Door Sensor", namespace: "CNSoft OnLine", author: "Clark Nelson") {
capability "Contact Sensor"
capability "Sensor"
capability "Refresh"

attribute "status", "string"
}


// UI tile definitions
tiles {
	standardTile("contact", "device.contact", width: 2, height: 2) {
		
	
		state "open", label:'${name}', icon:"st.contact.contact.open", backgroundColor:"#ffa81e"
        state "closed", label: '${name}', icon: "st.contact.contact.closed", backgroundColor: "#79b821"
		state "Failed", label: '${name}', icon: "st.contact.contact.open", backgroundColor: "#e5e500"
	}
	standardTile("refresh", "device.status", inactiveLabel: false, decoration: "flat") {
		state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh"

	}
	main "contact"
	details ("contact", "refresh", "bypass")
	}
}

// Zone status Information is below
// '' – Closed
// device_status.dc_open – Open


def baseUrl() {
	return "https://mob.yalehomesystem.co.uk/yapi/"
}
def endpointToken() {
	return "o/token/"
}
def endpointDevice() {
	return "api/panel/device_status/"
}
def yaleAuthToken () {
	return "VnVWWDZYVjlXSUNzVHJhcUVpdVNCUHBwZ3ZPakxUeXNsRU1LUHBjdTpkd3RPbE15WEtENUJ5ZW1GWHV0am55eGhrc0U3V0ZFY2p0dFcyOXRaSWNuWHlSWHFsWVBEZ1BSZE1xczF4R3VwVTlxa1o4UE5ubGlQanY5Z2hBZFFtMHpsM0h4V3dlS0ZBcGZzakpMcW1GMm1HR1lXRlpad01MRkw3MGR0bmNndQ=="
}

// ================================== Login/out Function. Returns cookie for rest of the functions =========
def login() {
	log.debug "Attempting to login"
	def paramsLogin = [
			uri: baseUrl() + endpointToken(),
			body: [grant_type: "password", username:settings.userName , password: settings.password],
			headers: ['Authorization' : "Basic ${yaleAuthToken()}"],
			requestContentType: "application/x-www-form-urlencoded",
			contentType: "application/json"
	]
	httpPost(paramsLogin) { responseLogin ->
		log.debug "Login response is $responseLogin.data"
		state.accessToken = responseLogin.data?.access_token
		state.refreshToken = responseLogin.data?.refresh_token
	}
	log.info "'$device' Logged in"
}
//
//def logout(token) {
//    def paramsLogout = [
//            uri: "https://www.yalehomesystem.co.uk/homeportal/api/logout/",
//            headers: ['Cookie' : "${token}"]
//    ]
//    httpPost(paramsLogout) { responseLogout ->
//    }
//    log.info "'$device' Logged out"
//}
// ================================================ Login /out end ========================



 // Gets Panel Metadata. Takes token & location ID as an argument
Map panelMetaData() {

def tczones

def getPanelMetaDataAndFullStatus = [
	uri: baseUrl() + endpointDevice(),
	headers: ['Authorization' : "Bearer ${state.accessToken}"]
]

httpGet(getPanelMetaDataAndFullStatus) { response ->
	log.debug "Zones Refresh - response = '$response.data'"
	tczones = response.data.data
}
return [tczones: tczones]
} //Should return Sensor and description Information


def refresh() {		   
	login()
def zname = device.name
def zonenumber = settings.zonenumber as int
def metaData = panelMetaData() // Gets Information
log.debug "Doing zone refresh"
log.debug metaData.tczones[zonenumber].name
if (metaData.tczones.contains("system.permission_denied")) {
	log.debug "Zone ${metaData.tczones} is Fault"
	sendEvent(name: "contact", value:"Failed", displayed: "true", description: "Refresh: Zone is Faulted", linkText: "Zone  ${zname} faulted", isStateChange: "true")
} else if (metaData.tczones[zonenumber].status1.contains('device_status.dc_open')) {
	log.debug "Zone ${metaData.tczones[zonenumber].status1} is OPEN"
	sendEvent(name: "contact", value:"open", displayed: "true", description: "Refresh: Zone is Open", linkText: "Zone ${metaData.tczones[zonenumber].status1} - ${zname}", isStateChange: "true")
} else if (metaData.tczones[zonenumber].status1.contains('')) {
	log.debug "Zone ${metaData.tczones[zonenumber].status1} is OK"
	sendEvent(name: "contact", value:"closed", displayed: "true", description: "Refresh: Zone is closed", linkText: "Zone ${metaData.tczones[zonenumber].status1} - ${zname}", isStateChange: "true")
}   
sendEvent(name: "refresh", value: "true", displayed: "true", description: "Refresh Successful") 
}

// parse events into attributes
def parse(String description) {
log.debug "Parsing '${description}'"
}