package traben.entity_model_features.models.animation.animation_math_parser;

import it.unimi.dsi.fastutil.chars.CharArrayList;
import it.unimi.dsi.fastutil.chars.CharListIterator;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import traben.entity_model_features.models.animation.EMFAnimation;
import traben.entity_model_features.utils.EMFUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MathExpressionParser {

    public static final MathComponent NULL_EXPRESSION = () -> Float.NaN;
    private static final List<MathAction> BOOLEAN_COMPARATOR_ACTIONS = List.of(MathAction.EQUALS, MathAction.SMALLER_THAN_OR_EQUALS, MathAction.SMALLER_THAN, MathAction.LARGER_THAN_OR_EQUALS, MathAction.LARGER_THAN, MathAction.NOT_EQUALS);
    private static final List<MathAction> BOOLEAN_LOGICAL_ACTIONS = List.of(MathAction.AND, MathAction.OR);
    private static final List<MathAction> MULTIPLICATION_ACTIONS = List.of(MathAction.MULTIPLY, MathAction.DIVIDE, MathAction.DIVISION_REMAINDER);
    private static final List<MathAction> ADDITION_ACTIONS = List.of(MathAction.ADD, MathAction.SUBTRACT);
    private final String originalExpression;
    private final boolean wasInvertedBooleanExpression;
    private final boolean isNegative;
    private final EMFAnimation calculationInstance;
    private MathComponent optimizedComponent = null;
    private CalculationList components;
    private boolean nextValueIsNegative = false;
    private String caughtExceptionString = null;

    private MathExpressionParser(String expressionString, boolean isNegative, EMFAnimation calculationInstance, boolean invertBoolean){
        this.isNegative = isNegative;
        this.calculationInstance = calculationInstance;

        wasInvertedBooleanExpression = invertBoolean;

        expressionString = expressionString.trim();
        expressionString = expressionString.replaceAll("\\s*", "");
        originalExpression = expressionString;

        components = new CalculationList();
        try {

            RollingReader rollingReader = new RollingReader();
            CharArrayList charList = new CharArrayList(expressionString.toCharArray());
            CharListIterator charIterator = charList.iterator();

            Character firstBooleanChar = null;
            while (charIterator.hasNext()) {
                char currentChar = charIterator.nextChar();
                MathAction asAction = MathAction.getAction(currentChar);

                //process the char after a boolean character. i.e.   !, =, &, |, <, > could turn into !=, ==, &&, ||, <=, >=
                if (firstBooleanChar != null) {
                    if (asAction == MathAction.BOOLEAN_CHAR) {
                        // confirmed double boolean character such as ==, !=, &&, ||, <=, >=
                        readDoubleBooleanAction(calculationInstance, firstBooleanChar, currentChar);
                        //now iterate once again manually to continue as a regular iteration
                        if (!charIterator.hasNext())
                            throw new MathComponent.EMFMathException("ERROR: boolean operator [" + firstBooleanChar + currentChar + "] at end of expression for [" + calculationInstance.animKey + "] in [" + calculationInstance.modelName + "].");
                        currentChar = charIterator.nextChar();
                        asAction = MathAction.getAction(currentChar);
                    } else {
                        // the last boolean character was a single character boolean such as !, =, &, |, <, >
                        // so process it and continue as normal
                        readLastSingleBooleanAction(calculationInstance, firstBooleanChar, rollingReader);
                    }
                    firstBooleanChar = null;
                }

                // this character might be the start of a two character boolean operator such as ==, !=, &&, ||, <=, >=
                // so we store it and continue to the next character
                if (asAction == MathAction.BOOLEAN_CHAR) {
                    firstBooleanChar = currentChar;
                }
                //critical that no elif here

                // if the character is a minus sign, at the start of the expression, or after another math action,
                // then it is a negative sign for the following value
                if (asAction == MathAction.SUBTRACT &&
                        // the character is a minus sign, at the start of the expression
                        ((components.isEmpty() && rollingReader.isEmpty())
                                // or after another math action i.e. 2/-4
                                || (!components.isEmpty() && components.getLast() instanceof MathAction && rollingReader.isEmpty()))) {
                    nextValueIsNegative = true;
                } else if (asAction == MathAction.NONE) {
                    //write the character normally
                    rollingReader.write(currentChar);
                } else {
                    // if the character is a bracket, then it is a method or a bracketed expression
                    if (asAction == MathAction.OPEN_BRACKET) {
                        readMethodOrBrackets(rollingReader, charIterator);
                    } else {//asAction should be only + - * / ^
                        //add last read data as variable if present
                        readVariableOrConstant(rollingReader);
                        //finally add the new math action to the end
                        if (rollingReader.isEmpty() && asAction != MathAction.BOOLEAN_CHAR) {
                            components.add(asAction);
                        }
                    }
                }
            }
            //add last read data as variable if present
            readVariableOrConstant(rollingReader);

            if (components.isEmpty())
                throw new MathComponent.EMFMathException("ERROR: math components found to be empty for [" + calculationInstance.animKey + "] in [" + calculationInstance.modelName + "]");

            //resolve unnecessary addition actions
            CalculationList newComponents = new CalculationList();
            MathComponent lastComponent = null;
            for (MathComponent component :
                    components) {
                if (lastComponent instanceof MathAction && component instanceof MathAction action) {
                    if (action != MathAction.ADD) {
                        newComponents.add(component);
                    }
                    //do not include unneeded addition action as there is no reason to and will mess with parser logic
                } else {
                    newComponents.add(component);
                }
                lastComponent = component;
            }
            if (newComponents.get(0) == MathAction.ADD) {
                newComponents.remove(0);
            }
            if (newComponents.size() != components.size()) components = newComponents;

            //this NEEDS to run
            validateAndOptimize();

        } catch (MathComponent.EMFMathException e) {
            caughtExceptionString = e.toString();
        } catch (Exception e) {
            caughtExceptionString = "EMF animation ERROR: for [" + calculationInstance.animKey + "] in [" + calculationInstance.modelName + "] cause [" + e + "].";
            //noinspection CallToPrintStackTrace
            e.printStackTrace();
        }
    }

    public static MathComponent getOptimizedExpression(String expressionString, boolean isNegative, EMFAnimation calculationInstance) {
        return getOptimizedExpression(expressionString, isNegative, calculationInstance, false);
    }

    private static MathComponent getOptimizedExpression(String expressionString, boolean isNegative, EMFAnimation calculationInstance, boolean invertBoolean) {
        try {
            MathExpressionParser expression = new MathExpressionParser(expressionString, isNegative, calculationInstance, invertBoolean);

            MathComponent optimized = expression.optimizedComponent;
            if (optimized == null) {
                return NULL_EXPRESSION;
            }

            //just a boolean inverting wrapper
            if (expression.wasInvertedBooleanExpression) {
                return () -> optimized.getResult() == 1 ? 0 : 1;
            }

            return optimized;
        } catch (Exception e) {
            return NULL_EXPRESSION;
        }
    }

    private void readLastSingleBooleanAction(final EMFAnimation calculationInstance, final Character firstBooleanChar, final RollingReader rollingReader) throws MathComponent.EMFMathException {
        if (firstBooleanChar == '!') {
            //likely a '!' for boolean variables so need to add to read
            rollingReader.write('!');
        } else {
            //add complete single char boolean action
            components.add(switch (firstBooleanChar) {
                case '=' -> MathAction.EQUALS;
                case '&' -> MathAction.AND;
                case '|' -> MathAction.OR;
                case '<' -> MathAction.SMALLER_THAN;
                case '>' -> MathAction.LARGER_THAN;
                default ->
                        throw new MathComponent.EMFMathException("ERROR: with boolean processing for operator [" + firstBooleanChar + "] for [" + calculationInstance.animKey + "] in [" + calculationInstance.modelName + "].");
            });
        }
    }

    private void readDoubleBooleanAction(final EMFAnimation calculationInstance, final Character firstBooleanChar, final char currentChar) throws MathComponent.EMFMathException {
        MathAction doubleAction = switch (firstBooleanChar + "" + currentChar) {
            case "==" -> MathAction.EQUALS;
            case "!=" -> MathAction.NOT_EQUALS;
            case "&&" -> MathAction.AND;
            case "||" -> MathAction.OR;
            case ">=" -> MathAction.LARGER_THAN_OR_EQUALS;
            case "<=" -> MathAction.SMALLER_THAN_OR_EQUALS;
            default ->
                    throw new MathComponent.EMFMathException("ERROR: with boolean processing for operator [" + firstBooleanChar + currentChar + "] for [" + calculationInstance.animKey + "] in [" + calculationInstance.modelName + "].");
        };
        //add complete double boolean action
        components.add(doubleAction);
    }

    private void readMethodOrBrackets(final RollingReader rollingReader, final CharListIterator charIterator) throws MathComponent.EMFMathException {
        String functionName = rollingReader.read();

        String bracketContents = readBracketContents(charIterator);

        if (functionName.isEmpty() || "!".equals(functionName) || "-".equals(functionName)) {
            //just nested brackets
            components.add(MathExpressionParser.getOptimizedExpression(
                    bracketContents,
                    getNegativeNext() || "-".equals(functionName),
                    this.calculationInstance,
                    "!".equals(functionName)));
        } else {
            //method
            components.add(MathMethod.getOptimizedExpression(functionName, bracketContents, getNegativeNext(), this.calculationInstance));
        }
    }

    private static String readBracketContents(final CharListIterator charIterator) {
        final StringBuilder bracketContents = new StringBuilder();
        int nesting = 0;
        while (charIterator.hasNext()) {
            char ch2 = charIterator.nextChar();
            if (ch2 == '(') {
                bracketContents.append(ch2);
                nesting++;
            } else if (ch2 == ')') {
                if (nesting == 0) {
                    break;
                } else {
                    bracketContents.append(ch2);
                    nesting--;
                }
            } else {
                bracketContents.append(ch2);
            }
        }
        return bracketContents.toString();
    }

    private void readVariableOrConstant(final RollingReader rollingReader) throws MathComponent.EMFMathException {
        if (!rollingReader.isEmpty()) {
            //discover rolling read value
            String read = rollingReader.read();
            try {
                //assume it is a number
                var asNumber = Float.parseFloat(read);
                components.add(new MathConstant(asNumber, getNegativeNext()));
            } catch (NumberFormatException ignored) {
                //otherwise it must be a text variable
                components.add(MathVariable.getOptimizedVariable(read, getNegativeNext(), this.calculationInstance));
            }
            //throws an additional exception if not a number or variable and not empty
        }
    }

    protected void validateAndOptimize() {
        if (caughtExceptionString != null) {
            EMFUtils.logWarn(caughtExceptionString);
            return;
        }

        //if the expression is not valid, then return NaN
        if (Double.isNaN(this.validateCalculationAndOptimize())) {
            EMFUtils.logWarn("result was NaN, expression not valid: " + originalExpression);
        }
    }

    private boolean getNegativeNext() {
        boolean neg = nextValueIsNegative;
        nextValueIsNegative = false;
        return neg;
    }

    private float validateCalculationAndOptimize() {

        //if only one component then it is a simple optimization
        if (components.size() == 1) {
            MathComponent comp = components.getLast();
            if (comp instanceof MathConstant constnt) {
                if (isNegative) comp = new MathConstant(-constnt.getResult());
            } else if (comp instanceof MathValue val) {
                val.isNegative = isNegative != val.isNegative;
            }
            optimizedComponent = comp;
            return comp.getResult();
        }

        try {

            // optimize the expression into binary expression tree components, following the order of operations
            CalculationList optimised =
                    optimiseTheseActionsIntoBinaryComponents(BOOLEAN_LOGICAL_ACTIONS,
                            optimiseTheseActionsIntoBinaryComponents(BOOLEAN_COMPARATOR_ACTIONS,
                                    optimiseTheseActionsIntoBinaryComponents(ADDITION_ACTIONS,
                                            optimiseTheseActionsIntoBinaryComponents(MULTIPLICATION_ACTIONS,
                                                    new CalculationList(components)))));


            if (optimised.size() == 1) {
                float result = optimised.getLast().getResult();
                if (Float.isNaN(result)) {
                    EMFUtils.logError(" result was NaN in [" + calculationInstance.modelName + "] for expression: " + originalExpression + " as " + components);
                } else {
                    //save optimized version of valid expression
                    optimizedComponent = optimised.getLast();
                    if (optimizedComponent instanceof MathValue value) {
                        value.makeNegative(this.isNegative);
                    }
                }
                return result;
            } else {
                EMFUtils.logError("ERROR: calculation did not result in 1 component, found: " + optimised + " in [" + calculationInstance.animKey + "] in [" + calculationInstance.modelName + "].");
                EMFUtils.logError("\texpression was [" + originalExpression + "].");
            }
        } catch (Exception e) {
            EMFUtils.logError("EMF animation ERROR: expression error in [" + calculationInstance.animKey + "] in [" + calculationInstance.modelName + "] caused by [" + e + "].");
        }

        //if the expression is not valid, then return NaN
        return Float.NaN;
    }

    private CalculationList optimiseTheseActionsIntoBinaryComponents(List<MathAction> actionsForThisPass, CalculationList componentsOptimized) {

        List<MathAction> containedActions = new ArrayList<>();
        for (MathAction forThisPass : actionsForThisPass) {
            if (componentsOptimized.contains(forThisPass)) {
                containedActions.add(forThisPass);
            }
        }

        if (!containedActions.isEmpty()) {
            CalculationList newComponents = new CalculationList();
            Iterator<MathComponent> compIterator = componentsOptimized.iterator();

            while (compIterator.hasNext()) {
                MathComponent component = compIterator.next();
                if (component instanceof MathAction action) {
                    if (containedActions.contains(action)) {
                        MathComponent last = newComponents.getLast();
                        MathComponent next = compIterator.next();
                        newComponents.removeLast();
                        newComponents.add(MathBinaryExpressionComponent.getOptimizedExpression(last, action, next));
                    } else {
                        newComponents.add(component);
                    }
                } else {
                    newComponents.add(component);
                }
            }
            return newComponents;
        }
        return componentsOptimized;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("{");
        for (MathComponent comp :
                components) {
            builder.append(comp.toString()).append(" ");
        }
        builder.append("}");
        return builder.toString();

    }

    private static class RollingReader {
        private StringBuilder builder = new StringBuilder();

        void clear() {
            builder = new StringBuilder();
        }

        void write(char ch) {
            builder.append(ch);
        }

        String read() {
            clear();
            return toString();
        }

        @Override
        public String toString() {
            return builder.toString();
        }

        boolean isEmpty() {
            return builder.isEmpty();
        }

    }

    private static class CalculationList extends ObjectArrayList<MathComponent> {
        public CalculationList(CalculationList components) {
            super(components);
        }

        public CalculationList() {
        }

        public MathComponent getLast() {
            return super.get(size - 1);
        }

        public void removeLast() {
            super.remove(size - 1);
        }
    }
}
