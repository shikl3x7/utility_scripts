'''
To get this working follow the steps 
1. Run this python script in background 
2. Add a new file in /tmp folder 
3. Background process stops by reporting changes in /tmp folder
'''
import pyinotify
import asyncio

def handle_read_callback(notifier):
    """
    Just stop receiving IO read events after the first
    iteration (unrealistic example).
    """
    print('handle_read callback')
    notifier.loop.stop()


wm = pyinotify.WatchManager()
loop = asyncio.get_event_loop()
notifier = pyinotify.AsyncioNotifier(wm, loop,
                                             callback=handle_read_callback)
wm.add_watch('/tmp', pyinotify.ALL_EVENTS)
loop.run_forever()
notifier.stop()
