import re

from zguide.mdcliapi2 import MajorDomoClient


class Stanford0mqClient(MajorDomoClient):


    def __init__(self, broker_addr, verbose=False):
        super(Stanford0mqClient, self).__init__(broker_addr, verbose)


    def _prepare_text(self, text):
        '''
        0mq/TCP sockets seem to do crazy stuff when we send over newlines ("\n").
        TODO: come up with a better way to handle this...
        '''
        if type(text) == list:
            return [("  " if t == "\n" else t) for t in text]
        else:
            is_tagged = re.search(r'[^/_]+([/_])[^/_ ]+', text)
            if is_tagged:
                delimiter = is_tagged.groups()[0]
                return [delimiter, text.replace("\n", "  ")]
            return text.replace("\n", "  ")


    def parse(self, text, outputFormat, outputFormatOptions):
        self.reconnect_to_broker()
        response = None
        text = self._prepare_text(text)
        while response is None:
            try:
                request = [outputFormat, outputFormatOptions]
                if type(text) == list:
                    request += text
                else:
                    request.append(text)
                self.send("ParserWorker", request)
                response = self.recv()
                if response is not None:
                    if type(text) == list:
                        return response[0]
                    else:
                        return response
            except Exception as e:
                response = None
                # TODO: Probably all sorts of exceptions could be thrown; handle them.
