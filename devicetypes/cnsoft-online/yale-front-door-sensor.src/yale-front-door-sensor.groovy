/**
*  Version 0.1 -  
*/
preferences {

input("userName", "text", title: "Username", description: "Your username for Yale Home System")
input("password", "password", title: "Password", description: "Your Password for Yale Home System")

input("zonenumber", type: "number", title: "Row Number - Put 0 in here and open a window/door, refresh and see if the status changes", description: "Sensor ID")
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


// Login Function. Returns SessionID for rest of the functions
def login(token) {
log.debug "Executed login"
def paramsLogin = [
	uri: "https://www.yalehomesystem.co.uk/homeportal/api/login/check_login/",
	body: [id:settings.userName , password: settings.password]
]
httpPost(paramsLogin) { responseLogin ->
	token = responseLogin.headers?.'Set-Cookie'?.split(";")?.getAt(0)
} 
return token
} // Returns cookie as token		


// Logout Function. Called after every mutational command. Ensures the current user is always logged Out.
def logout(token) {
//log.debug "During logout - ${token}"
def paramsLogout = [
	uri: "https://www.yalehomesystem.co.uk/homeportal/api/logout/",
	headers: ['Cookie' : "${token}"]
]
httpPost(paramsLogout) { responseLogout ->
	log.debug "Smart Things has successfully logged out"
}  
}



 // Gets Panel Metadata. Takes token & location ID as an argument
Map panelMetaData(token) {

def tczones

def getPanelMetaDataAndFullStatus = [
	uri: "https://www.yalehomesystem.co.uk/homeportal/api/panel/get_devices/",
	body: [id:settings.userName , password: settings.password],
	headers: ['Cookie' : "${token}"]
]

httpPost(getPanelMetaDataAndFullStatus) {	response ->

    tczones = response.data.message


}
return [tczones: tczones]
} //Should return Sesor and description Information


def refresh() {		   
def token = login(token)
def zname = device.name
def zonenumber = settings.zonenumber as int
def metaData = panelMetaData(token) // Gets Information
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
logout(token)
sendEvent(name: "refresh", value: "true", displayed: "true", description: "Refresh Successful") 
}

// parse events into attributes
def parse(String description) {
log.debug "Parsing '${description}'"
}