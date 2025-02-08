from infer import *
import sys

filename = sys.argv[1]
type = sys.argv[2]
threshold = sys.argv[3]

infer(filename, type, float(threshold))