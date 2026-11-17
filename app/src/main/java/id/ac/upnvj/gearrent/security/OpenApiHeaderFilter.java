package id.ac.upnvj.gearrent.security;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.eclipse.microprofile.openapi.models.parameters.Parameter;

/** Adds M2 tenant/role context headers to /api/* operations in Swagger UI. */
public class OpenApiHeaderFilter implements OASFilter {

    @Override
    public void filterOpenAPI(OpenAPI openAPI) {
        if (openAPI.getPaths() == null || openAPI.getPaths().getPathItems() == null) {
            return;
        }
        openAPI.getPaths().getPathItems().forEach((path, item) -> {
            if (!path.startsWith("/api")) {
                return;
            }
            for (Operation op : operationsOf(item)) {
                List<Parameter> params = op.getParameters() != null
                    ? new ArrayList<>(op.getParameters())
                    : new ArrayList<>();
                addHeaderIfMissing(params, "X-Tenant-Id", "Tenant identifier (e.g. tenant-a, tenant-b)", true, "tenant-a");
                addHeaderIfMissing(params, "X-User-Role", "Caller role (user, operator, admin)", true, "operator");
                addHeaderIfMissing(params, "X-User-Id", "Caller username (optional; defaults to role@tenant)", false, "operator1");
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
