# Browser Proxy

It is used by the autotest agent to collect the coverage from a remote test run. This proxy converts cookies
into headers that drill needs.

Default port is 7777. You can change it using the DRILL_PROXY_HTTP_PORT environment variable.

## Using in autotest-agent

Add "browserProxyAddress" parameter to autotest-agent configuration to start collecting coverage from a remote
test run.

