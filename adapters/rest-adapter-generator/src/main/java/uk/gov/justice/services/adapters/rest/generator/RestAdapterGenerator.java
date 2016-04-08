package uk.gov.justice.services.adapters.rest.generator;

import org.raml.model.Raml;
import org.raml.model.Resource;
import uk.gov.justice.raml.common.validator.CompositeRamlValidator;
import uk.gov.justice.raml.common.validator.ContainsActionsRamlValidator;
import uk.gov.justice.raml.common.validator.ContainsResourcesRamlValidator;
import uk.gov.justice.raml.common.validator.RamlValidator;
import uk.gov.justice.raml.common.validator.RequestContentTypeRamlValidator;
import uk.gov.justice.raml.core.Generator;
import uk.gov.justice.raml.core.GeneratorConfig;
import uk.gov.justice.services.adapters.rest.validator.ResponseContentTypeRamlValidator;

import java.io.File;
import java.util.Collection;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.apache.commons.lang.Validate.isTrue;
import static org.apache.commons.lang.Validate.notEmpty;
import static org.apache.commons.lang.Validate.notNull;

public class RestAdapterGenerator implements Generator {

    private final RamlValidator validator = new CompositeRamlValidator(
            new ContainsResourcesRamlValidator(),
            new ContainsActionsRamlValidator(),
            new RequestContentTypeRamlValidator(),
            new ResponseContentTypeRamlValidator()
    );



    @Override
    public void run(final Raml raml, final GeneratorConfig configuration) {
        validate(configuration);
        validator.validate(raml);

        Collection<Resource> resources = raml.getResources().values();

        JaxRsCodeGenerator codeGenerator = new JaxRsCodeGenerator(configuration);
        Collection<String> implementationNames = resources.stream()
                .map(resource -> {
                    final String interfaceName = codeGenerator.createInterface(resource);
                    return codeGenerator.createImplementation(interfaceName);
                })
                .collect(Collectors.toList());
        codeGenerator.createApplication(raml, implementationNames);
        codeGenerator.generate();
    }

    private void validate(final GeneratorConfig configuration) {
        notNull(configuration, "Configuration can't be null");
        notEmpty(configuration.getBasePackageName(), "Base package name can't be empty");

        final File outputDirectory = configuration.getOutputDirectory().toFile();
        notNull(outputDirectory, "OutputDirectory can't be null");
        isTrue(outputDirectory.isDirectory(), format("%s is not a pre-existing directory", outputDirectory));
        isTrue(outputDirectory.canWrite(), format("%s can't be written to", outputDirectory));
    }

}