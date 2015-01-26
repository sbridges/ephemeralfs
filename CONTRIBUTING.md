Contributing
============

To contribute please fork and submit a pull request.

Please ensure all tests and pmd/checkstyle pass before submitting a pull request by doing,

    mvn verify

Add tests as appropriate.  Most tests will run against the default file system 
implementation as well to ensure we match the implementation of the real file system.
You can do this by annotating test classes with,

    @RunWith(MultiFsRunner.class)

