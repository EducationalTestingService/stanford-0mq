from zguide.mdcliapi2 import MajorDomoClient


class Stanford0mqClient(MajorDomoClient):


    def __init__(self, broker_addr, verbose=False):
        super(Stanford0mqClient, self).__init__(broker_addr, verbose)


    def _prepare_text(self, text):
        '''
        0mq/TCP sockets seem to do crazy stuff when we send over newlines ("\n").
        TODO: come up with a better way to handle this...
        '''

        return text.replace("\n", "  ")


    def parse(self, text):
        # TODO: Determine whether or not `text` is a list of tokenized sentences
        # and send to appropriate ParserWorker method.
        response = None
        text = self._prepare_text(text)
        while True:
            try:
                self.send("ParserWorker", text)
                response = self.recv()
                if response is not None:
                    return response
                    break
            except Exception as e:
                # TODO: Probably all sorts of exceptions could be thrown; handle them.
                continue
