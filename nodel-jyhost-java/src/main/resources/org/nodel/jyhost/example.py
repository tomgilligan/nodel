'''This is an example node''' # this line appears as a short description line of text

# <!-- example 1: dynamic local events and actions using 'create_local_event' function and '@local_action' decorator

localPowerEvent = create_local_event('Power', {'group': 'Example 1', 'schema': {'type': 'string'}, 'order': 1})

@local_action({'group': 'Example 1', 'schema': {'type': 'string', 'enum': ['On', 'Off'], 'order': 1},
               'caution': 'This is a power operation so be careful!'})
def Power(arg):
  console.info('Power called. arg:"%s"' % arg)
  localPowerEvent.emit(arg)

@local_action({'group': 'Example 1', 'caution': 'This is a power operation so be careful!', 'desc': '(convenience version)', 'order': 1.1})
def PowerOn():
  Power.call('On')

@local_action({'group': 'Example 1', 'caution': 'This is a power operation so be careful!', 'desc': '(convenience version)', 'order': 1.2})
def PowerOff():
  Power.call('Off')
  
# --!> (example 1)



# <!-- example 2: static local events and actions using 'local_action_ / LocalAction()' and 'local_event_ / LocalEvent()' prefixes

local_event_Volume = LocalEvent({'group': 'Example 2', 'schema': {'type': 'integer'}, 'order': 2})

def local_action_SetVolume(arg):
  '''{"group": "Example 2", "schema": {"type": "integer"}, "order": 2}'''
  local_event_Volume.emit(arg)

def local_action_IncrVolume():
  '''{"group": "Example 2", "order": 2.1}'''
  vol = local_event_Volume.getArg() or 0
  lookup_local_action('SetVolume').call(max(vol + 1, 10))

def local_action_DecrVolume():
  '''{"group": "Example 2", "order": 2.2}'''
  vol = local_event_Volume.getArg() or 0
  lookup_local_action('SetVolume').call(min(vol - 1, 0))

# --!> (example 2)


# <!-- example 3: remote events and actions

def remote_event_RemotePowerSignal(arg):
  console.info('Got remote power:' % arg)

remote_action_RemotePowerAction = RemoteAction()

def local_action_RemotePowerControl(arg):
  '''{"group": "Example 3", "order": 3}'''
  console.info('Performing remote power action...')
  remote_action_RemotePowerAction.call(arg)

# --!> (remote events and actions)


# <!-- main usage

def main():
  console.info('The script has loaded successfully so main() is called!')

  # look up existing values
  console.info('The persisted local Power is: %s' % localPowerEvent.getArg())
  console.info('The persisted local Volume is: %s' % local_event_Volume.getArg())

@after_main
def anotherFuncion():
  console.info('(this is another function to be called after main())')

@after_main
def yetAnotherFuncion():
  console.info('(this is yet another function to be called after main())')

# --!>


# <!-- console examples

@local_action({'group': '(console examples)', 'order': next_seq(), 'schema': {'type': 'string'}, 'order': 98})
def logError(msg):
  console.error(msg)

@local_action({'group': '(console examples)', 'order': next_seq(), 'schema': {'type': 'string'}, 'order': 98.1})
def logWarn(msg):
  console.warn(msg)  

@local_action({'group': '(console examples)', 'order': next_seq(), 'schema': {'type': 'string'}, 'order': 98.2})
def logInfo(msg):
  console.info(msg)

# --!> (console)


# <!-- schema examples

@local_action({'group': '(schema examples)', 'order': next_seq(), 'schema': {'type': 'integer'}, 'order': 99})
def Integer(arg):
  if arg == None:
    console.warn('Integer: No argument provided')
  else:
    console.info('Integer called. arg:%s' % arg)

@local_action({'group': '(schema examples)', 'order': next_seq(), 'schema': {'type': 'boolean'}, 'order': 99.1})
def Boolean(arg):
  if arg == None:
    console.warn('Boolean: No argument provided')
  else:
    console.info('Boolean called. arg:%s' % arg)

@local_action({'group': '(schema examples)', 'order': next_seq(), 'schema': {'type': 'string', 'enum': ['On', 'Off']}, 'order': 99.2})
def StringEnum(arg):
  if arg == None:
    console.warn('BasicEnum: No argument provided')
  else:
    console.info('BasicEnum called. arg:"%s"' % arg)

# --!> (schema examples)






# Metadata fields for actions and events:
#  - 'title': (string) A short title different from the name
#  - 'order': (number) Force ordering relative to other actions/events
#  - 'caution':


