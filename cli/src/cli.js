import vorpal from 'vorpal'
import { words } from 'lodash'
import { connect } from 'net'
import { Message } from './Message'
export const cli = vorpal()
let username
let server
let host
let port
let firstSuccessfulCommand = false

cli
  .delimiter(cli.chalk['yellow']('ftd~$'))
cli
  .mode('connect <username> [host] [port]')
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
      const preString = Message.fromJSON(buffer)
      let postColor = ''
      if (preString.command === 'echo') {
        postColor = cli.chalk['green'](Message.fromJSON(buffer).toString())
      } else if (preString.command === 'broadcast') {
        postColor = cli.chalk['red'](Message.fromJSON(buffer).toString())
      } else if (preString.command === 'whisper' || preString.command.toString().startsWith('ATSIGN')) {
        postColor = cli.chalk['blue'](Message.fromJSON(buffer).toString())
      } else if (preString.command === 'connect' || preString.command === 'disconnect') {
        postColor = cli.chalk['yellow'](Message.fromJSON(buffer).toString())
      } else if (preString.command === 'users') {
        postColor = cli.chalk['bgGreen'](Message.fromJSON(buffer).toString())
      } else if (preString.command === 'lastcommand') {
        postColor = cli.chalk['bgBlue'](Message.fromJSON(buffer).toString())
      }
      this.log(postColor)
    })
    server.on('end', () => {
      cli.exec('exit')
    })
  })
  .action(function (input, callback) {
    const [ command, ...rest ] = words(input.replace(/@/g, 'ATSIGN'))
    const contents = rest.join(' ')
    if (command === 'disconnect') {
      firstSuccessfulCommand = true
      server.end(new Message({ username, command }).toJSON() + '\n')
    } else if (command === 'echo') {
      firstSuccessfulCommand = true
      server.write(new Message({ username, command, contents }).toJSON() + '\n')
    } else if (command === 'broadcast') {
      firstSuccessfulCommand = true
      server.write(new Message({ username, command, contents }).toJSON() + '\n')
    } else if (command === 'users') {
      firstSuccessfulCommand = true
      server.write(new Message({ username, command, contents }).toJSON() + '\n')
    } else if (command === 'lastcommand') {
      server.write(new Message({ username, command, contents }).toJSON() + '\n')
    } else if (input.startsWith('@') || input.startsWith('ATSIGN')) {
      // this.log('prepping private message')
      const newCommand = input.replace(/@/g, 'ATSIGN').split(' ')[0]
      firstSuccessfulCommand = true
      const moddedMsg = new Message({username, command: newCommand, contents})
      server.write(moddedMsg.toJSON() + '\n')
    } else {
      if (firstSuccessfulCommand === false) this.log('Please choose a valid command')
      server.write(new Message({ username, command, contents }).toJSON() + '\n')
    }
    callback()
  })
