/*
 * PhatJellyfin
 * Copyright (C) 2026 Enrique García (https://github.com/KGBis)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package io.github.kgbis.phatjellyfin.arguments;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterDescription;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.UnixStyleUsageFormatter;
import com.beust.jcommander.WrappedParameter;
import com.beust.jcommander.internal.Lists;
import io.github.kgbis.phatjellyfin.output.Console;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

import static io.github.kgbis.phatjellyfin.output.MessageManager.LOGO;

@Slf4j
public class CliUsageFormatter extends UnixStyleUsageFormatter {

    private static final String MAIN_PARAMETER_HELP = "Source directory. Quote the path if it contains spaces.";

    private final JCommander commander;

    private final Console console;

    public CliUsageFormatter(JCommander commander, Console console) {
        super(commander);
        this.commander = commander;
        this.console = console;
    }

    public void error(ParameterException exception) {
        StringBuilder sb = new StringBuilder();
        usage(sb, exception.getMessage(), "");
        console.println(sb.toString());
    }

    /**
     * Stores the usage in the argument string builder, with the argument indentation. This works by appending
     * each portion of the help in the following order. Their outputs can be modified by overriding them in a
     * subclass of this class.
     *
     * <ul>
     *     <li>Main line - {@link #appendMainLine(StringBuilder, String, boolean, boolean, int, String)}</li>
     *     <li>Parameters - {@link #appendAllParametersDetails(StringBuilder, int, String, List)}</li>
     *     <li>Commands - {@link #appendCommands(StringBuilder, int, int, String)}</li>
     * </ul>
     */
    public void usage(StringBuilder out, String error, String indent) {
        if (commander.getDescriptions() == null) {
            commander.createDescriptions();
        }
        boolean hasCommands = !commander.getCommands().isEmpty();
        boolean hasOptions = !commander.getDescriptions().isEmpty();

        // Indentation constants
        final int descriptionIndent = 6;
        final int indentCount = indent.length() + descriptionIndent;

        // Append first line (aka main line) of the usage
        appendMainLine(out, error, hasOptions, hasCommands, indentCount, indent);

        List<ParameterDescription> sortedParameters = Lists.newArrayList();

        for (ParameterDescription pd : commander.getFields().values()) {
            if (!pd.getParameter().hidden()) {
                sortedParameters.add(pd);
            }
        }

        // Sort the options
        sortedParameters.sort(commander.getParameterDescriptionComparator());

        // Append all the parameter names and descriptions
        appendAllParametersDetails(out, indentCount, indent, sortedParameters);

        // Append commands if they were specified
        if (hasCommands) {
            appendCommands(out, indentCount, descriptionIndent, indent);
        }
    }

    public void appendMainLine(StringBuilder out, String error, boolean hasOptions, boolean hasCommands, int indentCount, String indent) {
        out.append(LOGO).append("\n");
        if (StringUtils.isNotBlank(error)) {
            out.append(Console.RED)
                    .append("ERROR: ")
                    .append(error)
                    .append(Console.RESET)
                    .append("\n\n");
        }
        super.appendMainLine(out, hasOptions, hasCommands, indentCount, indent);

        if (commander.getMainParameter() != null && commander.getMainParameterDescription() != null) {
            appendMainParameter(out, indent);
        }
    }

    @Override
    public void appendMainLine(StringBuilder out, boolean hasOptions, boolean hasCommands, int indentCount, String indent) {
        appendMainLine(out, null, hasOptions, hasCommands, indentCount, indent);
    }

    private void appendMainParameter(StringBuilder out, String indent) {
        // Calculate prefix indent
        int prefixIndent = 0;

        for (ParameterDescription pd : commander.getParameters()) {
            WrappedParameter parameter = pd.getParameter();
            String prefix = (parameter.required() ? "* " : "  ") + pd.getNames();

            if (prefix.length() > prefixIndent) {
                prefixIndent = prefix.length();
            }
        }

        // Append the main parameter as "argument"
        String argDescriptionIndented = commander.getMainParameterDescription();
        out.append(indent).append("\n  Argument:\n");
        out.append(indent)
                .append("    ")
                .append(argDescriptionIndented)
                .append(s(prefixIndent - argDescriptionIndented.length() - 1))
                .append(MAIN_PARAMETER_HELP)
                .append("\n\n");
    }
}
