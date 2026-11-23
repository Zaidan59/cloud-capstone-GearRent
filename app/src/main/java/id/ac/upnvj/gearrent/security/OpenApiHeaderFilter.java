package id.ac.upnvj.gearrent.security;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.models.Components;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.eclipse.microprofile.openapi.models.parameters.Parameter;
import org.eclipse.microprofile.openapi.models.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.models.security.SecurityScheme;

/** Configures Bearer JWT security scheme and optional tenant selector header on /api/* in Swagger UI. */
public class OpenApiHeaderFilter implements OASFilter {

    private static final String BEARER_SCHEME_NAME = "bearerAuth";

    @Override
    public void filterOpenAPI(OpenAPI openAPI) {
        Components components = openAPI.getComponents();
        if (components == null) {
            components = OASFactory.createComponents();
            openAPI.setComponents(components);
        }
        components.addSecurityScheme(
            BEARER_SCHEME_NAME,
            OASFactory.createSecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("Keycloak OIDC access token from /realms/capstone/protocol/openid-connect/token")
        );

        if (openAPI.getPaths() == null || openAPI.getPaths().getPathItems() == null) {
            return;
        }
        openAPI.getPaths().getPathItems().forEach((path, item) -> {
            if (!path.startsWith("/api")) {
                return;
            }
            for (Operation op : operationsOf(item)) {
                op.addSecurityRequirement(OASFactory.createSecurityRequirement().addScheme(BEARER_SCHEME_NAME));
                List<Parameter> params = op.getParameters() != null
                    ? new ArrayList<>(op.getParameters())
                    : new ArrayList<>();
                addHeaderIfMissing(
                    params,
                    "X-Tenant-Id",
                    "Optional tenant selector for multi-tenant admin tokens (e.g. tenant-a, tenant-b)",
                    false,
                    "tenant-a"
                );
                op.setParameters(params);
            }
        });
    }

    private static List<Operation> operationsOf(PathItem item) {
        List<Operation> ops = new ArrayList<>();
        if (item.getGET() != null) ops.add(item.getGET());
        if (item.getPOST() != null) ops.add(item.getPOST());
        if (item.getPUT() != null) ops.add(item.getPUT());
        if (item.getDELETE() != null) ops.add(item.getDELETE());
        if (item.getPATCH() != null) ops.add(item.getPATCH());
        return ops;
    }

    private static void addHeaderIfMissing(
        List<Parameter> params, String name, String description, boolean required, String example
    ) {
        boolean exists = params.stream().anyMatch(p -> name.equalsIgnoreCase(p.getName()));
        if (exists) {
            return;
        }
        Schema schema = OASFactory.createSchema().type(List.of(Schema.SchemaType.STRING));
        Parameter param = OASFactory.createParameter()
            .name(name)
            .in(Parameter.In.HEADER)
            .required(required)
            .description(description)
            .example(example)
            .schema(schema);
        params.add(param);
    }
}
