ecm-extender-ri
===============

Implementation of ECM extender that picks up annotated ECM components based
on bundle capabilities.

## Usage

 - Add ecm-extender-ri to your dependencies
 - Annotate your classes with org.everit.osgi.ecm.annotation
 - Provide capabilities like the following

```
Provide-Capability: org.everit.osgi.ecm.component;class="org.everit.osgi.ecm.extender.ri.tests.TestComponent"
``` 