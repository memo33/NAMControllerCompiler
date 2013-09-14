package testsuites;

import model.ModelTest;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import controller.ControllerTest;

@RunWith(Suite.class)
@SuiteClasses({ModelTest.class, ControllerTest.class})
public class AllIntegrationTests {}
