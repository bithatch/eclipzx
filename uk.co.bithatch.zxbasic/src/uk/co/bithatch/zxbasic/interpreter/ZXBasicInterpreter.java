package uk.co.bithatch.zxbasic.interpreter;

import static uk.co.bithatch.zxbasic.interpreter.Var.forBoolean;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;

import uk.co.bithatch.zxbasic.basic.Assignment;
import uk.co.bithatch.zxbasic.basic.AttributeModifier;
//import uk.co.bithatch.zxbasic.basic.BNotExpr;
import uk.co.bithatch.zxbasic.basic.BeepStmt;
import uk.co.bithatch.zxbasic.basic.BinaryExpr;
import uk.co.bithatch.zxbasic.basic.CodeResource;
import uk.co.bithatch.zxbasic.basic.CommentStmt;
import uk.co.bithatch.zxbasic.basic.ConstStmt;
import uk.co.bithatch.zxbasic.basic.DataResource;
import uk.co.bithatch.zxbasic.basic.DataStmt;
import uk.co.bithatch.zxbasic.basic.DecimalLiteral;
import uk.co.bithatch.zxbasic.basic.DimDeclaration;
import uk.co.bithatch.zxbasic.basic.DimStmt;
import uk.co.bithatch.zxbasic.basic.DimValueInitialize;
import uk.co.bithatch.zxbasic.basic.DoLoopUntil;
import uk.co.bithatch.zxbasic.basic.DoLoopWhile;
import uk.co.bithatch.zxbasic.basic.DoWhileCondition;
import uk.co.bithatch.zxbasic.basic.DrawStmt;
import uk.co.bithatch.zxbasic.basic.ExitStmt;
//import uk.co.bithatch.zxbasic.basic.DimDeclaration;
//import uk.co.bithatch.zxbasic.basic.DimStmt;
import uk.co.bithatch.zxbasic.basic.Expr;
import uk.co.bithatch.zxbasic.basic.ForBlock;
import uk.co.bithatch.zxbasic.basic.FunctionBlock;
import uk.co.bithatch.zxbasic.basic.Gosub;
import uk.co.bithatch.zxbasic.basic.Goto;
import uk.co.bithatch.zxbasic.basic.IfStmt;
import uk.co.bithatch.zxbasic.basic.InStmt;
import uk.co.bithatch.zxbasic.basic.IntegralLiteral;
import uk.co.bithatch.zxbasic.basic.LoadStmt;
import uk.co.bithatch.zxbasic.basic.OnStmt;
import uk.co.bithatch.zxbasic.basic.OutStmt;
import uk.co.bithatch.zxbasic.basic.PlotStmt;
import uk.co.bithatch.zxbasic.basic.PokeStmt;
//import uk.co.bithatch.zxbasic.basic.NumberedLine;
import uk.co.bithatch.zxbasic.basic.PrintStmt;
import uk.co.bithatch.zxbasic.basic.Program;
import uk.co.bithatch.zxbasic.basic.RandomizeStmt;
import uk.co.bithatch.zxbasic.basic.ReadStmt;
import uk.co.bithatch.zxbasic.basic.Referable;
import uk.co.bithatch.zxbasic.basic.ReferableRef;
import uk.co.bithatch.zxbasic.basic.ReturnStmt;
import uk.co.bithatch.zxbasic.basic.RndStmt;
import uk.co.bithatch.zxbasic.basic.SaveStmt;
import uk.co.bithatch.zxbasic.basic.ScreenResource;
import uk.co.bithatch.zxbasic.basic.Statement;
import uk.co.bithatch.zxbasic.basic.StopStmt;
import uk.co.bithatch.zxbasic.basic.StringLiteral;
import uk.co.bithatch.zxbasic.basic.SubBlock;
import uk.co.bithatch.zxbasic.basic.VerifyStmt;
import uk.co.bithatch.zxbasic.interpreter.InterpreterHost.PrintAt;
import uk.co.bithatch.zxbasic.interpreter.InterpreterHost.PrintAttribute;
import uk.co.bithatch.zxbasic.interpreter.InterpreterHost.PrintInstruction;
import uk.co.bithatch.zxbasic.interpreter.InterpreterHost.PrintText;
import uk.co.bithatch.zxbasic.interpreter.util.ArrayUtils;
import uk.co.bithatch.zxbasic.tools.AbstractTool;

/**
 * ZX Basic Interpreter
 * 
 * TODO options like --explicit and --string etc --strict-boolean
 */
public class ZXBasicInterpreter extends AbstractTool  {
	
	private final static class Defaults  {
		private final static InterpreterHost defaultHost = new InterpreterHost() {

			@Override
			public String inkey() {
				return "";
			}

			@Override
			public void ink(int ink) {
			}

			@Override
			public void paper(int ink) {
			}

			@Override
			public void border(int ink) {
			}

			@Override
			public InputStream input() {
				return System.in;
			}

			@Override
			public void beep(float duration, int freq) {
			}

			@Override
			public void attr(AttributeModifier attributes) {
			}

			@Override
			public void at(int line, int column) {
			}

			@Override
			public void print(PrintInstruction... parts) {
				for(var part : parts) {
					if(part instanceof PrintText text) {
						System.out.print(text);		
					}
				}
				System.out.println();
			}

			@Override
			public void plot(int x, int y, AttributeModifier... attrs) {
			}

			@Override
			public void draw(int x, int y, Optional<Float> arg, AttributeModifier... attrs) {
			}

			@Override
			public void circle(int x, int y, int radius, AttributeModifier... attrs) {
			}

		};
	}
	
	/**
	 * Builder
	 */
	public final static class Builder extends AbstractBuilder<Builder, ZXBasicInterpreter> {
		
		private Optional<InterpreterHost> host = Optional.empty();
		private Optional<Z80Memory> memory = Optional.empty();
		private Optional<Z80IO> io = Optional.empty();

		public Builder withHost(InterpreterHost host) {
			this.host = Optional.of(host);
			return this;
		}
		
		public Builder withMemory(Z80Memory memory) {
			this.memory = Optional.of(memory);
			return this;
		}
		
		public Builder withIO(Z80IO io) {
			this.io = Optional.of(io);
			return this;
		}
		
		public ZXBasicInterpreter build() {
			return new ZXBasicInterpreter(this);
		}
	}
	
	private boolean debug = true;
	private final InterpreterHost host;
	private Thread runThread;
	private boolean stop;
	private DataIterator dataIterator;
	
	private final Z80Memory memory;
	private final SecureRandom rnd;
	private final Optional<Z80IO> io;
	private final Map<String, Referable> referables = new HashMap<>();
    
    private ZXBasicInterpreter(Builder bldr) {
    	super(bldr);
    	host = bldr.host.orElseGet(() -> Defaults.defaultHost);
    	memory = bldr.memory.orElseGet(() -> new Z80Memory.Builder().build());
    	rnd = new SecureRandom();
    	io = bldr.io;
    }
    
    public Var run(Program program) {
    	return runTerminable(() -> {
    		return doRun(program);
    	});
    }

    private Var doRun(Program program) {
        log("Finding referables");
        for(var grp : program.getProgram().getGroups()) {
        	if(grp instanceof FunctionBlock fb) {
        		if(referables.containsKey(fb.getName()))
        			throw new IllegalStateException(fb.getName() + " is already a " + referables.get(fb.getName()).getClass().getName());
        		referables.put(fb.getName(), fb);
        	}
        	else if(grp instanceof SubBlock sb) {
        		if(referables.containsKey(sb.getName()))
        			throw new IllegalStateException(sb.getName() + " is already a " + referables.get(sb.getName()).getClass().getName());
        		referables.put(sb.getName(), sb);
        	}
        }
        
        log("Starting program execution");
        var scope = new ProgramScope(program, program.getProgram().getGroups());
        initializeConstants(scope);
        try {
        	executeLines(scope);
        }
        catch(Stop stop) {}
        log("Program execution finished");
        return scope.popVar(ProgramScope.RETURN_VALUE);
    }
    
    private void initializeConstants(ProgramScope scope) {
    	scope.putConst("PI", Var.PI);
    }

	private void executeLines(ProgramScope iterator) {
        while (iterator.hasNext()) {
        	if(stop)
        		throw new Stop();
            var line = iterator.next();
            log("Executing line " + line);
        	if(executeStatements(line.getStatements(), iterator)) {
        		return;
        	}
        }
    }

    private boolean executeStatements(List<Statement> statements, ProgramScope scope) {
        for (Statement stmt : statements) {
        	if(stop)
        		throw new Stop();
	        log("Executing statement: " + stmt);
	        if (stmt instanceof ReturnStmt returnStmt) {
	            returnStatement(returnStmt, scope);
	            return false;
	        }
	        else if (stmt instanceof PrintStmt print) {
	        	printStatement(print, scope);
	        } else if (stmt instanceof Assignment let) {
	            letStatement(scope, let);
	        } else if (stmt instanceof Goto gotoStmt) {
	            gotoStatement(scope, gotoStmt);
	            return false;
	        } else if (stmt instanceof Gosub gosubStmt) {
	            gosubStatement(scope, gosubStmt);
	        }  else if (stmt instanceof OnStmt onStmt) {
	        	onStatement(scope, onStmt);
	            return false;
	        } else if (stmt instanceof StopStmt) {
	            log("STOP statement encountered");
	            throw new Stop();
	        } else if (stmt instanceof CommentStmt) {
//	            log("Comment: " + ((CommentStmt) stmt).getComment());
	        } else if (stmt instanceof IfStmt ifStmt) {
	        	ifStatement(scope, ifStmt);
	        } 
	        else if (stmt instanceof ForBlock forStmt) {
	            forStatement(scope, forStmt);
	        }
	        else if (stmt instanceof DoLoopWhile doLoopWhile) {
	        	doLoopWhile(scope, doLoopWhile);
	        }
	        else if (stmt instanceof DoLoopUntil doLoopUntil) {
	        	doLoopUntil(scope, doLoopUntil);
	        }
	        else if (stmt instanceof DimStmt dim) {
	            dimStatement(scope, dim);
	        }
	        else if (stmt instanceof ConstStmt cnst) {
	            constStatement(scope, cnst);
	        }
	        else if(stmt instanceof DataStmt) {
//	        	// Noop
	        }
	        else if(stmt instanceof PokeStmt poke) {
	        	pokeStatement(poke, scope);
	        }
	        else if(stmt instanceof ReadStmt read) {
	        	readStatement(read, scope);
	        }
	        else if(stmt instanceof LoadStmt load) {
	        	loadStatement(load, scope);
	        }
	        else if(stmt instanceof SaveStmt save) {
	        	saveStatement(save, scope);
	        }
	        else if(stmt instanceof VerifyStmt vstmt) {
	        	verifyStatement(vstmt);
	        }
	        else if(stmt instanceof RandomizeStmt rand) {
	        	randomizeStatement(rand, scope);
	        }
	        else if(stmt instanceof InStmt) {
	        	// Noop, acts as a function
	        }
	        else if(stmt instanceof OutStmt out) {
	        	outStatement(out, scope);
	        }
	        else if(stmt instanceof ExitStmt) {
	        	scope.exit();
	        }
	        else if(stmt instanceof ReferableRef fcall) {
	        	evaluateReferable(fcall, scope);
	        }
	        else if(stmt instanceof BeepStmt beep) {
	        	beepStatement(beep, scope);
	        }
	        else if(stmt instanceof PlotStmt plot) {
	        	plotStatement(plot, scope);
	        }
	        else if(stmt instanceof DrawStmt draw) {
	        	drawStatement(draw, scope);
	        }
	        else
	        	throw new UnsupportedOperationException("Unsupported statement. " + stmt.getClass().getName());
	        
        }
        return false;
    }

	protected void beepStatement(BeepStmt beep, ProgramScope scope) {
		host.beep(
				evaluateExpr(beep.getDuration(), scope).floatValue(), 
				evaluateExpr(beep.getDuration(), scope).intValue());
	}
	
	protected void randomizeStatement(RandomizeStmt rand, ProgramScope scope) {
		if(rand.getExpr() == null)
			rnd.reseed();
		else {
			rnd.setSeed(evaluateExpr(rand.getExpr(), scope).longValue());
		}
	}

	protected void loadStatement(LoadStmt load, ProgramScope scope) {
		var res = host.load(load.getName().getValue());
		try(var stream = res.channel()) {
			
			var type = load.getType();
			
			var addr = Optional.ofNullable(load.getStart()).
					map(expr -> evaluateExpr(expr, scope).intValue()).
					orElseGet(() -> {
				if(type instanceof ScreenResource) {
					return 16384;
				}
				else if(type instanceof CodeResource) {
					return 0;
				}
				else if(type instanceof DataResource) {
					return 0;
				}
				else {
					throw new UnsupportedOperationException();
				}
			});
			
			var len = Optional.ofNullable(load.getLength()).
					map(expr -> evaluateExpr(expr, scope).intValue()).
					orElseGet(() -> {
						if(type instanceof ScreenResource) {
							return 6912;
						}
						else if(type instanceof CodeResource) {
							return res.size().orElseThrow(() -> new IllegalStateException("Could not determine length of resource to load, and it was not specified."));
						}
						else if(type instanceof DataResource) {
							return 0;
						}
						else {
							throw new UnsupportedOperationException();
						}
					});
			
			if(type.equals("DATA")) {
				throw new UnsupportedOperationException("De-serialization not yet supported.");
			}
			
			var buf = ByteBuffer.allocate(1024);
			while(len > 0) {
				if(len < buf.capacity()) {
					buf.limit(len);
				}
				var rd = stream.read(buf);
				if(rd == -1) {
					break;
				}
				buf.flip();
				memory.put(addr += rd, buf, 0, rd);
				len -= rd;
				buf.clear();
			}
		}
		catch(Exception ex) {
			log("Failed to load. " + ex.getMessage());
			memory.poke(23610, 26);
		}
	}
	
	protected void verifyStatement(VerifyStmt load) {
		var res = host.load(load.getName().getValue());
		try(var stream = res.channel()) {
			
			var type = load.getType();
			
			int addr;
			if(type instanceof ScreenResource) {
				addr = 16384;
			}
			else if(type instanceof CodeResource) {
				addr = 0;
			}
			else if(type instanceof DataResource) {
				throw new UnsupportedOperationException("Data verification not yet supported.");
			}
			else {
				throw new UnsupportedOperationException();
			}
			
			int len;
			if(type instanceof ScreenResource) {
				len = 6912;
			}
			else if(type instanceof CodeResource) {
				len = res.size().orElseThrow(() -> new IllegalStateException("Could not determine length of resource to load, and it was not specified."));
			}
			else {
				throw new UnsupportedOperationException();
			}
			
			var buf = ByteBuffer.allocate(1024);
			while(len > 0) {
				if(len < buf.capacity()) {
					buf.limit(len);
				}
				var rd = stream.read(buf);
				if(rd == -1) {
					break;
				}
				
				buf.flip();
				while(buf.hasRemaining()) {
					var b = buf.get();
					if(b != memory.data().get(addr)) {
						throw new IOException("Mismatch at " + addr + ". Expected " + b);
					}
				}
				
				len -= rd;
				buf.clear();
			}
		}
		catch(Exception ex) {
			log("Failed to load. " + ex.getMessage());
			memory.poke(23610, 26);
		}
	}
	

	protected void saveStatement(SaveStmt save, ProgramScope scope) {
		try(var stream = host.save(save.getName().getValue())) {
			
			var type = save.getType();
			
			var addr = Optional.ofNullable(save.getStart()).
					map(expr -> evaluateExpr(expr, scope).intValue()).
					orElseGet(() -> {
				if(type instanceof ScreenResource) {
					return 16384;
				}
				else if(type instanceof CodeResource) {
					return 0;
				}
				else if(type instanceof DataResource) {
					/* TODO */
					throw new UnsupportedOperationException("Not yet implemented");
				}
				else {
					throw new UnsupportedOperationException();
				}
			});
			
			var len = Optional.ofNullable(save.getLength()).
					map(expr -> evaluateExpr(expr, scope).intValue()).
					orElseGet(() -> {
						if(type instanceof ScreenResource) {
							return 6912;
						}
						else if(type instanceof CodeResource) {
							throw new IllegalStateException("Start address of code must be supplied.");
						}
						else if(type instanceof DataResource) {
							throw new UnsupportedOperationException("Not yet implemented");
						}
						else {
							throw new UnsupportedOperationException();
						}
					});
			
			if(type.equals("DATA")) {
				throw new UnsupportedOperationException("De-serialization not yet supported.");
			}
			
			stream.write(memory.data().slice(addr, len));
		}
		catch(Exception ex) {
			log("Failed to load. " + ex.getMessage());
			memory.poke(23610, 26);
		}
	}

	protected void doLoopUntil(ProgramScope scope, DoLoopUntil doLoopBlock) {
		do {
			if(doLoopBlock.getBlock() != null) {
				scope.push(doLoopBlock.getBlock().getGroups());
		    	try {
		    		executeLines(scope);
		    	}
		    	finally {
		    		scope.pop();
		    	}			
			}
		} while(evaluateExpr(doLoopBlock.getCondition().getCondition(), scope).booleanValue() == doLoopBlock.getCondition() instanceof DoWhileCondition);
	}

	protected void doLoopWhile(ProgramScope scope, DoLoopWhile doLoopBlock) {
		while(evaluateExpr(doLoopBlock.getCondition().getCondition(), scope).booleanValue() == doLoopBlock.getCondition() instanceof DoWhileCondition) {
			scope.push(doLoopBlock.getBlock().getGroups());
	    	try {
	    		executeLines(scope);
	    	}
	    	finally {
	    		scope.pop();
	    	}
		}
	}

	protected void readStatement(ReadStmt read, ProgramScope scope) {
		read.getVariables().forEach(ref -> {
			var expr = dataIterator.next();
			scope.putVar(varName(ref.getRef()), evaluateExpr(expr, scope), evaluateArrayIndexes(ref.getArgs(), scope));
		});
	}

	protected int[] evaluateArrayIndexes(EList<Expr> args, ProgramScope scope) {
		return args.stream().
				mapToInt(expr -> evaluateExpr(expr, scope).intValue()).
				toArray();
	}

	protected void pokeStatement(PokeStmt poke, ProgramScope scope) {
		if(poke.getType() == null)
			memory.poke(
					evaluateExpr(poke.getAddress(), scope).intValue(),
					evaluateExpr(poke.getValue(), scope).intValue());
		else
			throw new UnsupportedOperationException("TODO poke specific type");
	}

	protected Var rndStatement(RndStmt poke, ProgramScope scope) {
		return Var.forObject(rnd.nextDouble());
	}

	protected Var inStatement(InStmt poke, ProgramScope scope) {
		return Var.forUByte(io.map(i -> i.in(evaluateExpr(poke.getExpr(), scope).intValue())).orElse(0));
	}

	protected void outStatement(OutStmt poke, ProgramScope scope) {
		io.ifPresent(i -> i.out(evaluateExpr(poke.getAddress(), scope).intValue(), evaluateExpr(poke.getValue(), scope).intValue()));
	}

	protected void returnStatement(ReturnStmt returnStmt, ProgramScope iterator) {
		log("Returning from subroutine");
		
		if(iterator.part() instanceof FunctionBlock) {
			iterator.returnScope(returnStmt.getExpr() == null ? Var.FALSE : evaluateExpr(returnStmt.getExpr(), iterator));	
		}
		else if(iterator.part() instanceof SubBlock) {
			iterator.returnScope();
		}
		else {
			// Gosub
			iterator.pop();
		}
		
	}

	protected void drawStatement(DrawStmt draw, ProgramScope scope) {
		host.draw(evaluateExpr(draw.getX(), scope).intValue(),
				evaluateExpr(draw.getY(), scope).intValue(),
				Optional.ofNullable(draw.getArc()).map(ex -> evaluateExpr(draw.getY(), scope).floatValue()),
				draw.getAttributes().toArray(new AttributeModifier[0]));
	}

	protected void plotStatement(PlotStmt plot, ProgramScope scope) {
		host.plot(evaluateExpr(plot.getX(), scope).intValue(),
				evaluateExpr(plot.getY(), scope).intValue(), 
				plot.getAttributes().toArray(new AttributeModifier[0]));
	}

	protected void printStatement(PrintStmt print, ProgramScope scope) {
		
		host.print(print.getParts().stream().map(p -> {
			if(p.getAttributes() != null)
				return new PrintAttribute(p.getAttributes());
			else if(p.getAt() != null)
				return new PrintAt(evaluateExpr(p.getAt().getAddresses().get(0), scope).intValue(),
						evaluateExpr(p.getAt().getAddresses().get(1), scope).intValue());
			else
				return new PrintText(evaluateExpr(p.getExpr(), scope).stringValue());
		}).toList().toArray(new PrintInstruction[0]));
	}

	protected void letStatement(ProgramScope scope, Assignment let) {
		Var value = evaluateExpr(let.getExpr(), scope);
		scope.putVar(let.getName(), value, evaluateArrayIndexes(let.getArgs(), scope));
		log("LET: " + let.getName() + " = " + value);
	}

	protected void onStatement(ProgramScope scope, OnStmt onStmt) {
		log("Processing ON");
		var labelIndex = evaluateExpr(onStmt.getExpression(), scope).intValue();
		if(labelIndex >= 0 && labelIndex < onStmt.getLabels().size()) {
			scope.gotoLine(onStmt.getLabels().get(labelIndex).getRef());
		}
		else {
			log("Skipping out of bounds ON statment. Evaluated to " +  labelIndex + ", there are " + onStmt.getLabels().size() + " labels");
		}
	}

	protected void gotoStatement(ProgramScope iterator, Goto gotoStmt) {
		log("Processing GOTO");
		var target = gotoStmt.getLine().getRef();
		if (target != null) {
		    log("GOTO target found: " + target);
		    iterator.gotoLine(target);
		} else {
		    throw new IllegalArgumentException("Invalid GOTO target");
		}
	}

	protected void gosubStatement(ProgramScope iterator, Gosub gosubStmt) {
		log("Processing GOSUB");
		var target = gosubStmt.getLine().getRef();
		if (target != null) {
		    log("GOSUB target found: " + target);
		    iterator.gosub(target);
		} else {
		    throw new IllegalArgumentException("Invalid GOSUB target");
		}
	}

	protected void ifStatement(ProgramScope scope, IfStmt ifStmt) {
		if (evaluateExpr(ifStmt.getCondition(), scope).booleanValue()) {
//			executeStatements(ifStmt.getStatements(), scope);
			scope.push(ifStmt.getThen().getGroups());
			try {
				executeLines(scope);
			}
			finally {
				scope.pop();
			}
			
		} else {
		    boolean matchedElseIf = false;
		    for (var elseif : ifStmt.getElseIf()) {
		        if (evaluateExpr(elseif.getCondition(), scope).booleanValue()) {
//		        	executeStatements(elseif.getBlock().getGroups(), scope);
		        	scope.push(elseif.getBlock().getGroups());
		        	try {
		            	executeLines(scope);
		                matchedElseIf = true;
		        	}
		        	finally {
		        		scope.pop();
		        	}
		            break;
		        }
		    }
		    if (!matchedElseIf && ifStmt.getElse() != null) {
//		    	executeStatements(ifStmt.getElse().getStatements(), scope);
		    	scope.push(ifStmt.getElse().getBlock().getGroups());
		    	try {
		    		executeLines(scope);
		    	}
		    	finally {
		    		scope.pop();
		    	}
		    }
		}
	}

	protected void constStatement(ProgramScope scope, ConstStmt cnst) {
		var initVal = evaluateExpr(cnst.getExpr(), scope);
		VarType type = cnst.getType() != null ? VarType.valueOf(cnst.getType().name()) : initVal.type();

		for (DimDeclaration decl : cnst.getDecls()) {
		    String name = decl.getName();
        	scope.putConst(name, enforceType(initVal, type));
            log("CONST: " + name + " AS " + type + " = " + initVal);
		}
	}
	
	protected void dimStatement(ProgramScope scope, DimStmt dim) {
		Var initVal = null;
		VarType type = null;

		for (DimDeclaration decl : dim.getDecls()) {
		    String name = decl.getName();
		    
		    if(type == null) {
		    	/* Is there an explicit type ?*/
		    	type = dim.getType() != null ? VarType.valueOf(dim.getType().name()) : null;
		    }
		    
		    if(initVal == null) {
			    var intlzr = dim.getInitializer();
				if(intlzr instanceof DimValueInitialize dvi) {
					initVal = evaluateExpr(dvi.getExpr().get(0), scope);
					if(type == null) {
						/* Derive type from initializer value type*/
						type = initVal.type();
					}
					else {
						/* Otherwise coerce type to specific type */
						initVal = initVal.toType(type);
					}
				}
				else if(intlzr != null) {
					if(!ArrayUtils.isArrayDeclaration(decl)) {
						throw new IllegalArgumentException("Not an array.");
					}
					
					if(type == null) {
				    	/* Need a type to create an array */ 
				    	if(name.endsWith("$"))
				    		type = VarType.STRING;
				    	else
				    		type = VarType.FLOAT;
				    }	
					
					var lbound = Arrays.asList(0);
			    	var ubound = Arrays.asList(intlzr.getExpr().size());
					var array =  newArrayDimension(type, lbound, ubound, (dimension,idx) -> {
						return evaluateExpr(intlzr.getExpr().get(idx), scope);
					}, 0);
			        var arrayValue = new ArrayValue(type, array, lbound, ubound); 
			        initVal = new Var(VarType.ARRAY, arrayValue);
				}
		    }
		    else if(type == null) {
		    	/* Still no known type type, base on name */ 
		    	if(name.endsWith("$"))
		    		type = VarType.STRING;
		    	else
		    		type = VarType.FLOAT;
		    }
		    
		    if (ArrayUtils.isArrayDeclaration(decl)) {
		    	var indices = decl.getArrayIndex().getIndices();
		    	var finitVal = initVal;
		    	var ftype = type;
		    	
				int size = dimArray(
		    		name, 
		    		type.nativeType, 
		    		type, 
		    		indices.stream().map(bnd -> evaluateExpr(bnd.getLower(), scope).intValue()).toList(), 
		    		indices.stream().map(bnd -> evaluateExpr(bnd.getUpper(), scope).intValue()).toList(),
		    		(dimension, idx) -> {
		    			if(finitVal == null) 
		    				return ftype.defaultValue();
		    			else {
		    				return finitVal.element(dimension, idx);
		    			}
		    		},
		    		scope);
		        log("DIM: array " + name + "[" + size + "] AS " + type);
		    } else {
	        	scope.putVar(name, enforceType(initVal, type));
	            log("DIM: " + name + " AS " + type + " = " + initVal);
		    }
		}
	}

	protected void forStatement(ProgramScope scope, ForBlock forStmt) {
		var varName = forStmt.getAssignment().getName();
		var start = evaluateExpr(forStmt.getAssignment().getExpr(), scope);
		var end = evaluateExpr(forStmt.getEnd(), scope);
		var step = forStmt.getStep() != null ? evaluateExpr(forStmt.getStep(), scope) : Var.forUInteger(1);
		
		if(step.zero())
			return;
		
		
		log("FOR loop " + varName + " from " + start + " to " + end + " step " + step);
		
		/* TODO arrays */
		var indexes = evaluateArrayIndexes(forStmt.getAssignment().getArgs(), scope);
		var wasValue = scope.getVar(varName, indexes);
		var bestType = wasValue == null ? VarType.best(start.type(), end.type(), step.type()) : wasValue.type();
		
		start = start.toType(bestType);
		end = end.toType(bestType);
		step = end.toType(bestType);
		
		try {
			var val = new Var(bestType, start.value());
			while(true) {
				if(step.gt(Var.ZERO) && val.gt(end)) {
					break;
				} else if(val.lt(end)) {
					break;
				}

		    	scope.putVar(varName, val, indexes);
		    	executeStatements(forStmt.getStatements(), scope);
		    	scope.push(forStmt.getBlock().getGroups());
		    	try {
		    		executeLines(scope);
		    	}
		    	finally {
		    		scope.pop();
		    	}
				
				val = val.add(step);
			}
		}
		finally {
			if(wasValue == null)
				scope.removeVar(varName);
		}
	}

    
	private <T> int dimArray(String name, Class<T> nativeType, VarType varType, List<Integer> lbound, List<Integer> ubound, BiFunction<Integer, Integer, Var> initVal, ProgramScope iterator) {
    	
    	T[] array =  newArrayDimension(varType, lbound, ubound, initVal, 0);
        ArrayValue arrayValue = new ArrayValue(varType, array, lbound, ubound); 
        iterator.putVar(name, new Var(VarType.ARRAY, arrayValue));
        
        return array.length;
    }

	@SuppressWarnings("unchecked")
	private <T> T[] newArrayDimension(VarType varType, List<Integer> lbound, List<Integer> ubound, BiFunction<Integer, Integer, Var> initVal, int dim) {
		int dimensions = lbound.size();
		int thisLbound = lbound.get(dim);
		int thisUbound = ubound.get(dim);
		if(thisUbound == 0) {
			thisUbound = thisLbound;
			thisLbound = 0;
		}
		int size = thisUbound - thisLbound;
		
		T[] array = varType.newArray(size);
		for (int i = 0; i < array.length; i++) {
			if(dim + 1 == dimensions) {
				array[i] = (T)enforceType(initVal.apply(dim, i), varType);
			}
			else {
				array[i] = (T)newArrayDimension(varType, lbound,  ubound, initVal, dim + 1);
			}
		}
		
		return array;
	}
    
    /**
     * Converts between zx basic and native types and makes sure they are within bounds
     * 
     * @param value original value
     * @param type target type
     * @return typed value
     */
    private Var enforceType(Var value, VarType type) {
    	return value.toType(type);
	}

	private Var evaluateReferable(ReferableRef fcall, ProgramScope scope) {
		var ref = referables.get(varName(fcall.getRef()));
		var args = fcall.getArgs();
		if (ref instanceof FunctionBlock fblock) {
//    	
			if (args.size() != fblock.getArgs().size())
				throw new IllegalArgumentException();

			var funcScope = scope.newScope(fblock, fblock.getBlock().getGroups());
			executeLines(funcScope);
			return funcScope.popVar(ProgramScope.RETURN_VALUE);
		} else if (ref instanceof SubBlock sblock) {
//        	
			if (args.size() != sblock.getArgs().size())
				throw new IllegalArgumentException();

			var funcScope = scope.newScope(sblock, sblock.getBlock().getGroups());
			executeLines(funcScope);
		} else {
			throw new UnsupportedOperationException("TODO");
//			switch(fcall.getName().toUpperCase()) {
//			case "ABS":
//				return ZXStdlib.abs(evaluateExpr(args.get(0), scope));
//			case "ASN":
//				return ZXStdlib.asn(evaluateExpr(args.get(0), scope));
//			case "ATN":
//				return ZXStdlib.atn(evaluateExpr(args.get(0), scope));
//			case "CAST":
//				return ZXStdlib.cast(evaluateExpr(args.get(0), scope));
//			case "CHR$":
//			case "CHR":
//				return ZXStdlib.chr(evaluateExpr(args.get(0), scope));
//			case "CODE":
//				return ZXStdlib.code(evaluateExpr(args.get(0), scope));
//			case "COS":
//				return ZXStdlib.cos(evaluateExpr(args.get(0), scope));
//			case "EXP":
//				return ZXStdlib.exp(evaluateExpr(args.get(0), scope));
//			case "INKEY$":
//			case "INKEY":
//				return Var.forString(host.inkey());
//			case "INPUT":
//				var in = new InputStreamReader(host.input());
//				var result = new StringBuilder();
//				var maxlen = args.size() == 0 ? Integer.MAX_VALUE : evaluateExpr(args.get(0), scope).intValue();
//				char rd;
//				try {
//					while (result.length() < maxlen && ( rd = (char)in.read() ) != -1) {
//						result.append((char)rd);
//					}
//					in.read();
//					return Var.forString(result.toString());
//				}
//				catch(IOException ioe) {
//					throw new UncheckedIOException(ioe);
//				}
//			case "INT":
//				return ZXStdlib.integer(evaluateExpr(args.get(0), scope));
//			case "LBOUND":
//				return ZXStdlib.lbound(evaluateExpr(args.get(0), scope));
//			case "UBOUND":
//				return ZXStdlib.ubound(evaluateExpr(args.get(0), scope));
//			case "LEN":
//				return ZXStdlib.len(evaluateExpr(args.get(0), scope));
//			case "LN":
//				return ZXStdlib.ln(evaluateExpr(args.get(0), scope));
//			case "PEEK":
//				if(args.size() > 1)
//					return memory.peek(VarType.valueOf(
//							evaluateExpr(args.get(1), scope).stringValue().toUpperCase()), 
//							evaluateExpr(args.get(1), scope).intValue());
//				else
//					return Var.forUByte(memory.peek(evaluateExpr(args.get(0), scope).intValue()));
//			case "SGN":
//				return ZXStdlib.sgn(evaluateExpr(args.get(0), scope));
//			case "SIN":
//				return ZXStdlib.sin(evaluateExpr(args.get(0), scope));
//			case "SQR":
//				return ZXStdlib.sqr(evaluateExpr(args.get(0), scope));
//			case "STR$":
//			case "STR":
//				return ZXStdlib.str(evaluateExpr(args.get(0), scope));
//			case "TAN":
//				return ZXStdlib.tan(evaluateExpr(args.get(0), scope));
//			case "USR":
//			case "USR$":
//				return ZXStdlib.usr(evaluateExpr(args.get(0), scope));
//			case "VAL":
//				return ZXStdlib.val(evaluateExpr(args.get(0), scope));
//			default:
//				throw new IllegalArgumentException("No such FUNCTION or SUB");
//			}
		}
		return Var.FALSE;
	}

    private Var evalStatement(Statement expr, ProgramScope scope) {
    	if(expr instanceof InStmt inStmt)
    		return inStatement(inStmt, scope);
    	else if(expr instanceof RndStmt rndStmt)
    		return rndStatement(rndStmt, scope);
    	else if(expr instanceof ReferableRef fcall)
    		return evaluateReferable(fcall, scope);
    	else
    		return Var.FALSE;
    }

    private Var evaluateExpr(Expr expr, ProgramScope scope) {
        if (expr instanceof DecimalLiteral) {
            return Var.forFixedOrFloat(((DecimalLiteral) expr).getValue());
        }
        else if (expr instanceof StringLiteral) {
            return Var.forString(((StringLiteral) expr).getValue());
        }
        else if (expr instanceof IntegralLiteral) {
            return Var.forIntegral(((IntegralLiteral) expr).getValue());
        } 
//        else if (expr instanceof NotExpr) {
//            return Var.forBoolean(!evaluateExpr(((NotExpr) expr).getExpr(), scope).booleanValue());
//        } 
//        else if (expr instanceof BNotExpr) {
//        	var eval = evaluateExpr(((BNotExpr) expr).getExpr(), scope);
//            return new Var(eval.type(), ~eval.longValue());
//        } 
        else if (expr instanceof BinaryExpr b) {
            /* TODO binary types, NOT, AND, OR */
            var leftVal = evaluateExpr(b.getLeft(), scope);
            var rightVal = evaluateExpr(b.getRight(), scope);
            if(leftVal.type() == VarType.STRING || rightVal.type() == VarType.STRING) {
	            switch (b.getOp()) {
	            
	            // TODO string comparison
	            
	            case "+":
	            	return Var.forString(leftVal.stringValue() + rightVal.stringValue());
	            } 
            }
            else {
				if(leftVal.type().integral()) {
		            switch (b.getOp()) {
		            	case "BAND": new Var(leftVal.type(), leftVal.longValue() & rightVal.longValue());
		            	case "BOR": new Var(leftVal.type(), leftVal.longValue() | rightVal.longValue());
		            	case "BXOR": new Var(leftVal.type(), leftVal.longValue() ^ rightVal.longValue());
	                	case "OR": Var.forBoolean(leftVal.booleanValue() || rightVal.booleanValue());
	                	case "AND": Var.forBoolean(leftVal.booleanValue() && rightVal.booleanValue());
		                case "+": return new Var(leftVal.type(), leftVal.longValue() + rightVal.longValue());
		                case "-": return new Var(leftVal.type(), leftVal.longValue() - rightVal.longValue());
		                case "*": return new Var(leftVal.type(), leftVal.longValue() * rightVal.longValue());
		                case "/": return rightVal.longValue() != 0 ? new Var(leftVal.type(), leftVal.longValue() / rightVal.longValue()) : Var.FALSE;
		                case "MOD": new Var(leftVal.type(), leftVal.longValue() % rightVal.longValue());
		                case "NOT": forBoolean(!leftVal.booleanValue());
		                case "=": return forBoolean(leftVal.longValue() == leftVal.longValue());
		                case "<>": return forBoolean(leftVal.longValue() != rightVal.longValue());
		                case "<": return forBoolean(leftVal.longValue() < rightVal.longValue());
		                case ">": return forBoolean(leftVal.longValue() > rightVal.longValue());
		                case "<=": return forBoolean(leftVal.longValue() <= rightVal.longValue());
		                case ">=": return forBoolean(leftVal.longValue() >= rightVal.longValue());
		                default:
		                	throw new UnsupportedOperationException();
		            }
				}
				else if(leftVal.type().floatingPoint()) {
		            switch (b.getOp()) {
		                case "+": return new Var(leftVal.type(), leftVal.doubleValue() + rightVal.doubleValue());
		                case "-": return new Var(leftVal.type(), leftVal.doubleValue() - rightVal.doubleValue());
		                case "*": return new Var(leftVal.type(), leftVal.doubleValue() * rightVal.doubleValue());
		                case "/": return rightVal.doubleValue() != 0 ? new Var(leftVal.type(), leftVal.doubleValue() / rightVal.doubleValue()) : Var.FALSE;
		                case "MOD": new Var(leftVal.type(), leftVal.doubleValue() % rightVal.doubleValue());
		                case "=": return Var.forBoolean(leftVal.doubleValue() == leftVal.doubleValue());
		                case "<>": return Var.forBoolean(leftVal.doubleValue() != rightVal.doubleValue());
		                case "<": return Var.forBoolean(leftVal.doubleValue() < rightVal.doubleValue());
		                case ">": return Var.forBoolean(leftVal.doubleValue() > rightVal.doubleValue());
		                case "<=": return Var.forBoolean(leftVal.doubleValue() <= rightVal.doubleValue());
		                case ">=": return Var.forBoolean(leftVal.doubleValue() >= rightVal.doubleValue());
		                default:
		                	throw new UnsupportedOperationException();
		            }
				}
            }
        }
        else if (expr instanceof Statement stmt) {
            return evalStatement(stmt, scope);
        }
        else if(expr instanceof ReferableRef varRef) {
        	return scope.getVar(varName(varRef.getRef()), evaluateArrayIndexes(varRef.getArgs(), scope));
        }
        else if(expr != null) {
        	throw new UnsupportedOperationException("Unknown expression type " + expr.getClass().getName());
        }
        
        return Var.FALSE;
    }
    private String varName(EObject obj) {
    	if(obj instanceof Referable r) {
    		return r.getName();
    	}
    	else
    		throw new IllegalStateException("Not a reference.");
    }

//    private String evaluateAsString(Object val) {
//    	if(val == null) {
//    		return "";
//    	}
//    	else if(val.getClass().isArray()) {
//    		throw new IllegalArgumentException("Cannot evaluate array as string."); 
//    	}
//    	else {
//    		return String.valueOf(val);
//    	}
//    }
//    
//    private Number evaluateAsNumber(Object val) {
//    	if(val == null) {
//    		return 0;
//    	}
//    	else if(val.getClass().isArray()) {
//    		throw new IllegalArgumentException("Cannot evaluate array as number."); 
//    	}
//    	else if(val instanceof Number nval) {
//    		return nval;
//    	}
//    	else 
//    		throw new IllegalArgumentException("Cannot evaluate " + val + " (" + val.getClass().getName() + ") as a number");
//    }
//    
//    private boolean evaluateAsBoolean(Object val) {
//    	if(val == null) {
//    		return false;
//    	}
//    	else if(val instanceof Number nval) {
//    		return nval.doubleValue() != 0;
//    	}
//    	else if(val instanceof String sval) {
//    		return sval.length() > 0;
//    	}
//    	else 
//    		throw new IllegalArgumentException("Cannot evaluate " + val + " as a boolean");
//    }
    
//    @SuppressWarnings("unchecked")
//	private static <T> T[] castArray(Class<T> clazz, Object[] arrayObject) {
//    	return (T[])arrayObject;
//    }

	public static ZXBasicInterpreter create() {
		return new Builder().build();
	}
	
	final static class GotoEscape extends RuntimeException {
	}
}
