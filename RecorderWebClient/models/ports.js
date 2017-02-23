


var Ports = []

const portOffset = 30000
const rangeNumber = 1000


for( i=0; i <= rangeNumber; i++) {

    Ports.push({
        number: portOffset + i,
        available: true
    })
}

Ports.getAvailable = function() {

    for( i=0; i <= rangeNumber; i++) {
        if (Ports[i].available === true)
            return Ports[i].number
    }
    return null
}

Ports.disavailablePort = function(number) {

    this[number-portOffset].available = false
}


Ports.availablePort = function(number) {

    this[number-portOffset].available = true
}



module.exports = Ports

