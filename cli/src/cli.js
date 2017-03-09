import vorpal from 'vorpal'
import { words } from 'lodash'
import { connect } from 'net'
import { Message } from './Message'
export const cli = vorpal()
let username
let server
let host
let port

cli
  .delimiter(cli.chalk['yellow']('ftd~$'))
cli
  .mode('connect <username> <host> <port>')
  .delimiter(cli.chalk['green']('connected>'))
  .init(function (args, callback) {
    username = args.username
    host = args.host || 'localhost'
    port = args.port || '8080'
    server = connect({ host, port }, () => {
      server.write(new Message({ username, command: 'connect' }).toJSON() + '\n')
      callback()
    })
    server.on('data', (buffer) => {
      this.log(Message.fromJSON(buffer).toString())
    })
    server.on('end', () => {
      cli.exec('exit')
    })
  })
  .action(function (input, callback) {
    const [ command, ...rest ] = words(input.replace(/@/g, "ATSIGN"))
    const contents = rest.join(' ')
    if (command === 'disconnect') {
      server.end(new Message({ username, command }).toJSON() + '\n')
    } else if (command === 'echo') {
      server.write(new Message({ username, command, contents }).toJSON() + '\n')
    } else if (command === 'broadcast') {
      server.write(new Message({ username, command, contents }).toJSON() + '\n')
    //} else if (input.startsWith('@') && !input.substring('1').startsWith('@')) {
      //let command2 = input.substring(1)
      //if (command2 !== undefined) {
        //command2 = "**" + command2
        //this.log(command2)
        //server.write(new Message({ username, command, contents }).toJSON() + '\n')
      //}
    } else if (input.startsWith('@') || input.startsWith("ATSIGN")) {
      this.log({ username, command, contents })
      const newCommand = input.replace(/@/g, "ATSIGN").split(" ")[0]
      this.log("newCommand: " + newCommand)
      const moddedMsg = new Message({ username, command: newCommand, contents})
        server.write(moddedMsg.toJSON() + '\n')
    } else {
      this.log(`Command <${command}> was not recognized`)
    }
    callback()
  })
