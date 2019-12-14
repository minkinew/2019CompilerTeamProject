int compare(int a, int b) {
    if(a > b) {
        return 1;
    } else {
        return 0;
    }
}
int add(int x, int y) {
	int z ;
	z = x+y;
	return z;
}

int sum(int d) {
    int q = 1;
    int result = 0;
    while (q < d) {
        result = result + q;
        q = q + 1;
    }
    return result;
}

void main () {
	int t = 33;
	_print(add(1,t));
	_print(compare(6,3));
	_print(sum(5));
}