package org.schabi.newpipe.extractor.utils;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

public final class JavaScript {

    private JavaScript() {
    }

    public static void compileOrThrow(final String function) {
        Value value;
        final Context context = Context.create();
        try {
              final Source source = Source.create("js", function);
              value = context.parse(source);
        } finally {
            context.close(true);
        }
    }

    public static String run(final String function,
                             final String functionName,
                             final String... parameters) {
        final Context context = Context.create();
        try {
            final Source source = Source.create("js", function);
            final Value value = context.eval("js", function);

            Value result = value.execute((Object) parameters);
            return result.toString();
        } finally {
            context.close(true);
        }
    }

}
