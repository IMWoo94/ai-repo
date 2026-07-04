package com.imwoo.airepo.wallet.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "com.imwoo.airepo.wallet", importOptions = ImportOption.DoNotIncludeTests.class)
class LayerDependencyTest {

    @ArchTest
    static final ArchRule domain_does_not_depend_on_outer_layers = noClasses()
            .that()
            .resideInAPackage("..domain..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                    "com.imwoo.airepo.wallet.api..",
                    "com.imwoo.airepo.wallet.application..",
                    "com.imwoo.airepo.wallet.infra..",
                    "com.imwoo.airepo.wallet.config..",
                    "org.springframework.."
            );

    @ArchTest
    static final ArchRule application_does_not_depend_on_adapters = noClasses()
            .that()
            .resideInAPackage("..application..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                    "com.imwoo.airepo.wallet.api..",
                    "com.imwoo.airepo.wallet.infra..",
                    "com.imwoo.airepo.wallet.config..",
                    "org.springframework.web..",
                    "org.springframework.jdbc..",
                    "org.springframework.security.."
            );

    @ArchTest
    static final ArchRule api_does_not_depend_on_infra_or_config = noClasses()
            .that()
            .resideInAPackage("..api..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("com.imwoo.airepo.wallet.infra..", "com.imwoo.airepo.wallet.config..");

    @ArchTest
    static final ArchRule infra_does_not_depend_on_api = noClasses()
            .that()
            .resideInAPackage("..infra..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("com.imwoo.airepo.wallet.api..");
}
