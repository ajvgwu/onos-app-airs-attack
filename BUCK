COMPILE_DEPS = [
    '//lib:CORE_DEPS',
    '//core/api:onos-api',
    '//lib:org.apache.karaf.shell.console',
    '//cli:onos-cli',
]

osgi_jar_with_tests (
    deps = COMPILE_DEPS,
)

onos_app (
    title = 'AIRS-ONOS Attack App',
    category = 'Test',
    url = 'https://www.nrl.navy.mil/itd/chacs/5542',
    description = 'AIRS-ONOS application for executing attacks.',
)
