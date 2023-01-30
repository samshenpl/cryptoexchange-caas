#!/bin/bash

#############################################################################
# Please adjust the terminationGracePeriodSeconds in the helm/values*.yaml
# set terminationGracePeriodSeconds longer than the process cleanup time
#############################################################################

# Wait 10s to let agency-server finish any outstanding requests
sleep 10

echo `date` STOP CAAS server
# Send signal to stop aeron service
kill -SIGINT 1

# Wait 5s to finish shutdown of aeron service
sleep 5