### IOPS AWS Lambda FHIR Validation

This is based on AWS FHIRWorks java lambda validator https://github.com/awslabs/fhir-works-on-aws-deployment/tree/mainline/javaHapiValidatorLambda
and is aimed at being deployed with AWS FHIRWorks. It is also capable of being run as a standalone module (as a spring boot application)

The main difference is the implementationGuides are in raw NPM format and so do not need to be unzipped (so this unzip documentation https://github.com/awslabs/fhir-works-on-aws-deployment/blob/mainline/USING_IMPLEMENTATION_GUIDES.md does not need to be followed)
It is recommended the Implementation Guide packages are sourced from IOPS AWS FHIRWorks server (not FHIR Registry or simplifier, documentation to follow), which preprocesses the NPM packages to reduce load times

If terminology testing is required or other features, please log issues on this github repository.





