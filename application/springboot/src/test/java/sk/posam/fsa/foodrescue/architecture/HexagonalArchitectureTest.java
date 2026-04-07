package sk.posam.fsa.foodrescue.architecture;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Set;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class HexagonalArchitectureTest {

    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("sk.posam.fsa.foodrescue");

    @Test
    void domain_must_not_depend_on_frameworks_or_adapters() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        "org.springframework..",
                        "jakarta.persistence..",
                        "org.hibernate..",
                        "sk.posam.fsa.foodrescue.controller..",
                        "sk.posam.fsa.foodrescue.mapper..",
                        "sk.posam.fsa.foodrescue.security..",
                        "sk.posam.fsa.foodrescue.jpa..",
                        "sk.posam.fsa.foodrescue")
                .because("domain must stay technologically agnostic");

        rule.check(CLASSES);
    }

    @Test
    void inbound_layer_must_not_depend_on_outbound_adapter_or_runtime() {
        ArchRule rule = noClasses()
                .that().resideInAnyPackage(
                        "sk.posam.fsa.foodrescue.controller..",
                        "sk.posam.fsa.foodrescue.mapper..",
                        "sk.posam.fsa.foodrescue.security..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        "sk.posam.fsa.foodrescue.jpa..",
                        "sk.posam.fsa.foodrescue")
                .because("REST inbound must delegate to domain, not to outbound or runtime");

        rule.check(CLASSES);
    }

    @Test
    void outbound_layer_must_not_depend_on_inbound_api_contract_or_runtime() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("sk.posam.fsa.foodrescue.jpa..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        "sk.posam.fsa.foodrescue.controller..",
                        "sk.posam.fsa.foodrescue.mapper..",
                        "sk.posam.fsa.foodrescue.security..",
                        "sk.posam.fsa.foodrescue.rest..",
                        "sk.posam.fsa.foodrescue")
                .because("JPA adapters must stay technical and not know inbound/runtime details");

        rule.check(CLASSES);
    }

    @Test
    void runtime_root_package_must_only_contain_application_and_configuration_classes() {
        ArchRule rule = classes()
                .that().resideInAPackage("sk.posam.fsa.foodrescue")
                .should().beAnnotatedWith(org.springframework.context.annotation.Configuration.class)
                .orShould().beAnnotatedWith(SpringBootApplication.class)
                .because("runtime root package must contain only bootstrapping and bean configurations");

        rule.check(CLASSES);
    }

    @Test
    void controllers_must_be_explicit_rest_entry_points() {
        ArchRule rule = classes()
                .that().resideInAPackage("sk.posam.fsa.foodrescue.controller..")
                .and().areTopLevelClasses()
                .should().beAnnotatedWith(RestController.class)
                .orShould().beAnnotatedWith(RestControllerAdvice.class)
                .because("controller package must contain only REST entry points and centralized error handling");

        rule.check(CLASSES);
    }

    @Test
    void repository_ports_and_adapters_must_follow_hexagonal_naming_and_roles() {
        ArchRule domainPorts = classes()
                .that().resideInAPackage("sk.posam.fsa.foodrescue.domain..")
                .and().haveSimpleNameEndingWith("Repository")
                .should().beInterfaces()
                .because("domain repository ports must be interfaces");

        ArchRule outboundAdapters = classes()
                .that().resideInAPackage("sk.posam.fsa.foodrescue.jpa..")
                .and().haveSimpleNameEndingWith("RepositoryAdapter")
                .should().beAnnotatedWith(Repository.class)
                .andShould(implementDomainRepositoryPort())
                .because("outbound repository adapters must be Spring repository beans implementing a domain port");

        domainPorts.check(CLASSES);
        outboundAdapters.check(CLASSES);
    }

    @Test
    void spring_data_repositories_must_stay_internal_to_outbound_module() {
        ArchRule rule = classes()
                .that().resideInAPackage("sk.posam.fsa.foodrescue.jpa..")
                .and().haveSimpleNameEndingWith("SpringDataRepository")
                .should().beInterfaces()
                .andShould().notBePublic()
                .because("Spring Data repositories are internal technical details of the outbound adapter");

        rule.check(CLASSES);
    }

    private static ArchCondition<JavaClass> implementDomainRepositoryPort() {
        return new ArchCondition<>("implement a domain repository port") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                Set<String> repositoryInterfaces = item.getAllRawInterfaces().stream()
                        .map(JavaClass::getFullName)
                        .filter(name -> name.startsWith("sk.posam.fsa.foodrescue.domain."))
                        .filter(name -> name.endsWith("Repository"))
                        .collect(java.util.stream.Collectors.toSet());

                boolean satisfied = !repositoryInterfaces.isEmpty();
                String message = item.getName() + (satisfied
                        ? " implements domain port " + repositoryInterfaces
                        : " does not implement any domain repository port");
                events.add(new SimpleConditionEvent(item, satisfied, message));
            }
        };
    }
}
