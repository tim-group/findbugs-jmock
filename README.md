# FindBugs4JMock #

[FindBugs](http://findbugs.sourceforge.net/) plugin which can be used to detect faulty [jMock](http://www.jmock.org/) tests. Copyright 2010 [youDevise, Ltd.](http://www.youdevise.com).

# Installation #

With a configured and running FindBugs installation, download the [FindBugs4JMock jar](https://github.com/downloads/youdevise/findbugs-jmock/findbugs4jmock-0.1.jar) and place it in the 'plugin' directory of your FindBugs installation. FindBugs will automatically enable the JAR the next time an analysis is run.

# What does the plugin do? #

The plugin will detect likely programming errors in unit tests using jMock. 

An example of a faulty test could be:

    //@RunWith(JMock.class)
    public class TestWithoutAssertingExpectations {
    
        @Test public void
        dontAssertContextIsSatisfied() {
            Mockery context = new Mockery();
            final MyMockedInterface mocked = context.mock(MyMockedInterface.class);
            context.checking(new Expectations() {{
                oneOf(mocked).expectedMethodCall();
            }});
        
            // do something which will invoke mocked.expectedMethodCall();
        
            // context.assertIsSatisfied();
        }
    }
    
The problem here is the test does not have either of:

 * a call to 'context.assertIsSatisfied()'
 * the @RunWith(JMock.class) annotation applied to the test class
	
This means the test will pass regardless of whether or not a call is made to 'mocked.expectedMethodCall()' - this is likely to be a programming error.

# Building from source #

The project can be built with [Maven](http://maven.apache.org/).

'mvn package' is required to build the plugin jar with the relevant config files included.

# License #

Open source under the very permissive [MIT license](https://github.com/youdevise/findbugs-jmock/blob/master/LICENSE).

# Acknowledgements #

A project of [TIM Group](http://www.timgroup.com). We're [hiring](http://www.timgroup.com/careers)!


